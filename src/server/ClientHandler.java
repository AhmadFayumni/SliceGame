package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final GameEngine game;
    private final Leaderboard leaderboard;
    private PrintWriter out;
    private BufferedReader in;
    public String currentUser;
    private boolean authenticated = false;

    public ClientHandler(Socket socket, GameEngine game, Leaderboard leaderboard) {
        this.socket = socket;
        this.game = game;
        this.leaderboard = leaderboard;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Failed to set up streams: " + e.getMessage());
            out = null;
            in = null;
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) return;
        try {
            String encryptedLine;
            while ((encryptedLine = in.readLine()) != null) {
                try {
                    String decrypted = CryptoUtil.decrypt(encryptedLine);
                    String[] parts = decrypted.split("\\|");
                    String command = parts[0];

                    if (!authenticated) {
                        if (command.equals("LOGIN") && parts.length == 3) {
                            handleLogin(parts[1], parts[2]);
                        } else if (command.equals("REGISTER") && parts.length == 3) {
                            handleRegister(parts[1], parts[2]);
                        } else if (command.equals("MFA_CONFIRM") && parts.length == 2) {
                            handleMFAConfirm(parts[1]);
                        } else {
                            sendMessage("ERROR|Please login or register first.");
                        }
                        continue;
                    }

                    switch (command) {
                        case "SLASH" -> {
                            int fruitId = Integer.parseInt(parts[1]);
                            int points = game.slashFruit(fruitId, currentUser, leaderboard);
                            if (points > 0) sendMessage("SCORE|" + game.getScore(currentUser));
                        }
                        case "CHAT" -> handleChat(parts[1]);
                        case "TELL" -> {
                            if (parts.length == 3) handleTell(parts[1], parts[2]);
                            else sendMessage("ERROR|Usage: TELL|username|message");
                        }
                        case "DELETE_ACCOUNT" -> handleDeleteAccount();
                        case "RESTART" -> game.forceRestart();
                        case "GET_LEADERBOARD" -> sendLeaderboard();
                        default -> sendMessage("ERROR|Unknown command.");
                    }
                } catch (Exception e) {
                    System.err.println("[ClientHandler] Error processing command: " + e.getMessage());
                    try {
                        sendMessage("ERROR|" + e.getMessage());
                    } catch (Exception ignored) {
                        System.err.println("[ClientHandler] Failed to send error message.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + currentUser);
            Server.clients.remove(this);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (currentUser != null) game.playerDisconnected();
        }
    }

    private void handleRegister(String user, String pass) throws Exception {
        if (Server.registerUser(user, pass)) {
            sendMessage("REGISTER_SUCCESS|Account created! Please login.");
        } else {
            sendMessage("REGISTER_FAIL|Username already exists.");
        }
    }

    private void handleLogin(String user, String pass) throws Exception {
        if (Server.authenticate(user, pass)) {
            currentUser = user;
            String puzzleOrder = Server.generateMFAPuzzle();
            sendMessage("MFA_REQUIRED|" + puzzleOrder);
            System.out.println("[MFA] Challenge sent to " + user + ": " + puzzleOrder);
        } else {
            sendMessage("AUTH_FAIL|Invalid credentials.");
        }
    }

    private void handleMFAConfirm(String order) throws Exception {
        if (Server.isPuzzleSolved(order)) {
            authenticated = true;
            sendMessage("AUTH_SUCCESS");
            game.playerConnected();
            String initial = game.getInitialFruits();
            if (!initial.isEmpty()) {
                for (String spawn : initial.split(",")) {
                    if (!spawn.isEmpty()) sendMessage(spawn);
                }
            }
            sendMessage("SCORE|" + game.getScore(currentUser));
            Server.broadcast("CHAT|System|" + currentUser + " joined the dojo!");
        } else {
            sendMessage("MFA_FAIL|Puzzle not solved. Try again.");
        }
    }

    private void handleChat(String message) {
        try {
            if (!message.startsWith("/")) {
                Server.broadcast("CHAT|" + currentUser + "|" + message);
            } else {
                if (message.startsWith("/tell ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) handleTell(parts[1], parts[2]);
                    else sendMessage("ERROR|Usage: /tell username message");
                } else if (message.equals("/deleteaccount")) {
                    handleDeleteAccount();
                } else {
                    sendMessage("ERROR|Unknown command. Use /tell or /deleteaccount");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in handleChat: " + e.getMessage());
        }
    }

    private void handleTell(String target, String msg) {
        try {
            boolean found = false;
            for (ClientHandler ch : Server.clients) {
                if (target.equals(ch.currentUser) && ch != this) {
                    ch.sendMessage("PRIVATE|" + currentUser + "|" + msg);
                    sendMessage("PRIVATE_SENT|To " + target + ": " + msg);
                    found = true;
                    break;
                }
            }
            if (!found) sendMessage("ERROR|User '" + target + "' not online.");
        } catch (Exception e) {
            System.err.println("Error in handleTell: " + e.getMessage());
        }
    }

    private void handleDeleteAccount() {
        try {
            if (Server.deleteUser(currentUser)) {
                sendMessage("ACCOUNT_DELETED|Your account has been removed.");
                Server.broadcast("CHAT|System|" + currentUser + " deleted their account.");
                socket.close();
            } else {
                sendMessage("ERROR|Failed to delete account.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleDeleteAccount: " + e.getMessage());
        }
    }

    private void sendLeaderboard() throws Exception {
        String data = leaderboard.getTopScores();
        sendMessage("LEADERBOARD_DATA|" + data);
    }

    public void sendMessage(String plainText) throws Exception {
        if (out == null) return;
        String encrypted = CryptoUtil.encrypt(plainText);
        out.println(encrypted);
    }

    public void sendRaw(String encrypted) {
        if (out != null) out.println(encrypted);
    }
}