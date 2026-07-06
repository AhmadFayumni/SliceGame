package server;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameEngine implements Runnable {
    private final Map<Integer, Fruit> fruits = new ConcurrentHashMap<>();
    private final Map<String, Integer> streaks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSlashTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> comboCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private int nextId = 1;
    private boolean running = true;
    private int gameState = 0;
    private long roundEndTime = 0;
    private static final int ROUND_DURATION = 60;
    private long lastTimeBroadcast = 0;
    private final Random random = new Random();
    private int spawnInterval = 800;
    private long frenzyEndTime = 0;
    private int connectedPlayers = 0;

    public void start() { new Thread(this).start(); }
    public void stop() { running = false; }

    public synchronized void playerConnected() {
        connectedPlayers++;
        if (connectedPlayers >= 2 && (gameState == 0 || gameState == 2)) {
            startRoundIfNeeded();
        } else {
            Server.broadcast("WAITING|Waiting for other players... (" + connectedPlayers + "/2)");
        }
    }

    public synchronized void playerDisconnected() {
        connectedPlayers--;
        if (connectedPlayers < 0) connectedPlayers = 0;
    }

    public synchronized void startRoundIfNeeded() {
        if ((gameState == 0 || gameState == 2) && connectedPlayers >= 2) {
            gameState = 1;
            roundEndTime = System.currentTimeMillis() + (ROUND_DURATION * 1000L);
            fruits.clear();
            // Reset combos but keep scores? Let's reset combos but keep scores across rounds.
            streaks.clear();
            comboCount.clear();
            lastSlashTime.clear();
            // We keep scores cumulative across rounds.
            nextId = 1;
            spawnInterval = 800;
            frenzyEndTime = 0;
            Server.broadcast("GAME_STARTING|🍉 Fruits are flying! Slice them!");
            System.out.println("🍉 New round started!");
        }
    }

    public synchronized void forceRestart() {
        if (gameState == 2) {
            gameState = 0;
            startRoundIfNeeded();
        }
    }

    public boolean isActive() { return gameState == 1; }

    @Override
    public void run() {
        long lastSpawn = 0;
        while (running) {
            long now = System.currentTimeMillis();

            if (gameState == 1) {
                if (now > roundEndTime) {
                    gameState = 2;
                    fruits.clear();
                    Server.broadcast("GAME_OVER|⏰ Time's up! Click Retry.");
                    System.out.println("⏰ Round finished.");
                    continue;
                }

                if (now - lastTimeBroadcast > 1000) {
                    int secondsLeft = (int) ((roundEndTime - now) / 1000);
                    if (secondsLeft < 0) secondsLeft = 0;
                    Server.broadcast("TIME|" + secondsLeft);
                    lastTimeBroadcast = now;
                }

                boolean frenzyActive = (now < frenzyEndTime);
                int currentInterval = frenzyActive ? 400 : spawnInterval;

                if (now - lastSpawn > currentInterval) {
                    spawnFruit();
                    lastSpawn = now;
                }
            }

            fruits.values().forEach(Fruit::update);
            fruits.entrySet().removeIf(e -> !e.getValue().alive);

            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
    }

    private void spawnFruit() {
        int radius = 25 + random.nextInt(16);
        double x = 50 + random.nextDouble() * 500;
        double y = 350 + random.nextDouble() * 150;
        double vy = -(4 + random.nextDouble() * 4);
        double vx = (random.nextDouble() - 0.5) * 6;
        int type = random.nextInt(4);
        Fruit f = new Fruit(nextId++, x, y, radius, vx, vy, type);
        fruits.put(f.id, f);
        String msg = "FRUIT_SPAWN|" + f.id + "|" + f.x + "|" + f.y + "|" + f.vx + "|" + f.vy + "|" + f.radius + "|" + f.type;
        Server.broadcast(msg);
    }

    public synchronized int slashFruit(int fruitId, String username, Leaderboard leaderboard) {
        if (gameState != 1) return 0;

        Fruit f = fruits.get(fruitId);
        if (f == null || !f.alive || f.isExpired()) {
            comboCount.put(username, 0);
            Server.sendToUser(username, "SLASH_RESULT|MISS|❌ Missed! Combo reset.");
            return 0;
        }

        f.alive = false;
        fruits.remove(fruitId);

        // Combo
        long now = System.currentTimeMillis();
        long last = lastSlashTime.getOrDefault(username, 0L);
        int combo = comboCount.getOrDefault(username, 0);
        if (now - last <= 1000) {
            combo++;
        } else {
            combo = 1;
        }
        comboCount.put(username, combo);
        lastSlashTime.put(username, now);

        int basePoints = switch (f.type) {
            case 0 -> 10;
            case 1 -> 12;
            case 2 -> 15;
            default -> 20;
        };

        int multiplier = 1;
        String bonusTag = "";
        if (combo >= 5) {
            multiplier = 2;
            bonusTag = "🔥 FRENZY!";
            frenzyEndTime = now + 5000;
        } else if (combo >= 3) {
            multiplier = (int)(1.5);
            bonusTag = "💥 COMBO x" + combo;
        }

        int points = basePoints * multiplier;
        int totalScore = scores.getOrDefault(username, 0) + points;
        scores.put(username, totalScore);

        // Update leaderboard
        leaderboard.updateScore(username, totalScore);

        Server.broadcast("FRUIT_SLICED|" + fruitId + "|" + username + "|" + combo + "|" + points + "|" + bonusTag);
        Server.sendToUser(username, "SLASH_RESULT|SUCCESS|+" + points + " pts " + bonusTag);
        return points;
    }

    public int getScore(String username) {
        return scores.getOrDefault(username, 0);
    }

    public String getInitialFruits() {
        StringBuilder sb = new StringBuilder();
        for (Fruit f : fruits.values()) {
            if (f.alive && !f.isExpired()) {
                sb.append("FRUIT_SPAWN|").append(f.id).append("|")
                        .append(f.x).append("|").append(f.y).append("|")
                        .append(f.vx).append("|").append(f.vy).append("|")
                        .append(f.radius).append("|").append(f.type).append(",");
            }
        }
        return sb.toString();
    }
}