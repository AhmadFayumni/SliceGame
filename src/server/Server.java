package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 12345;
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static GameEngine game;
    public static Leaderboard leaderboard;

    public static final Map<String, String> users = new ConcurrentHashMap<>();

    static {
        users.put("admin", "admin");
        users.put("user1", "pass1");
        users.put("user2", "pass2");
    }

    public static void main(String[] args) {
        System.out.println("🚀 Starting Fruit Ninja Server...");
        try {
            leaderboard = new Leaderboard();
            game = new GameEngine();
            game.start();
            System.out.println("✅ Game Engine started.");

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("🍉 Fruit Ninja Server started on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("👤 New client connected: " + clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket, game, leaderboard);
                    clients.add(handler);
                    handler.start();
                }
            } catch (java.net.BindException e) {
                System.err.println("❌ Port " + PORT + " already in use!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String plainText) {
        try {
            String encrypted = CryptoUtil.encrypt(plainText);
            for (ClientHandler ch : clients) {
                ch.sendRaw(encrypted);
            }
        } catch (Exception e) {
            System.err.println("Broadcast error: " + e.getMessage());
        }
    }

    public static void sendToUser(String username, String plainText) {
        try {
            String encrypted = CryptoUtil.encrypt(plainText);
            for (ClientHandler ch : clients) {
                if (ch.currentUser != null && ch.currentUser.equals(username)) {
                    ch.sendRaw(encrypted);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Send to user error: " + e.getMessage());
        }
    }

    public static List<String> getConnectedUsers() {
        List<String> list = new ArrayList<>();
        for (ClientHandler ch : clients) {
            if (ch.currentUser != null) list.add(ch.currentUser);
        }
        return list;
    }

    public static synchronized boolean registerUser(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        return true;
    }

    public static synchronized boolean authenticate(String username, String password) {
        String stored = users.get(username);
        return stored != null && stored.equals(password);
    }

    public static synchronized boolean deleteUser(String username) {
        return users.remove(username) != null;
    }

    // MFA utilities
    public static String generateMFAPuzzle() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) list.add(i);
        Collections.shuffle(list);
        if (list.stream().allMatch(i -> list.indexOf(i) == i)) {
            Collections.swap(list, 0, 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i : list) sb.append(i).append(",");
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static boolean isPuzzleSolved(String order) {
        String[] parts = order.split(",");
        if (parts.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            try {
                if (Integer.parseInt(parts[i]) != i) return false;
            } catch (NumberFormatException e) { return false; }
        }
        return true;
    }
}