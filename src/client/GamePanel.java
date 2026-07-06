package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel {
    private final Map<Integer, Fruit> fruits = new ConcurrentHashMap<>();
    private final FruitNinjaClientGUI parent;
    private String lastMessage = "";
    private int timeLeft = 60;
    private boolean gameActive = true;
    private int combo = 1;
    private int score = 0;
    private String waitingMessage = "";

    private final List<Point> trailPoints = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Set<Integer> slashedThisDrag = new HashSet<>();

    private String bigMessage = "";
    private int bigMessageLife = 0;
    private Color bigMessageColor = Color.WHITE;

    private static final Color BG_TOP = new Color(20, 30, 45);
    private static final Color BG_BOTTOM = new Color(10, 15, 25);
    private static final String[] FRUIT_EMOJIS = {"🍎", "🍊", "🍌", "🍉"};

    public GamePanel(FruitNinjaClientGUI parent) {
        this.parent = parent;
        setPreferredSize(new Dimension(450, 500));
        setFocusable(true);
        requestFocusInWindow();

        // Mouse drag to slice
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                trailPoints.clear();
                slashedThisDrag.clear();
                trailPoints.add(e.getPoint());
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                slashedThisDrag.clear();
                trailPoints.clear();
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!gameActive || !waitingMessage.isEmpty()) {
                    setLastMessage("⏳ Wait for the game to start!");
                    return;
                }
                Point p = e.getPoint();
                trailPoints.add(p);
                if (trailPoints.size() > 30) trailPoints.remove(0);

                for (Fruit f : fruits.values()) {
                    if (f.contains(p) && !slashedThisDrag.contains(f.id)) {
                        parent.sendMessage("SLASH|" + f.id);
                        slashedThisDrag.add(f.id);
                        fruits.remove(f.id);
                    }
                }
                repaint();
            }
        });

        // Quick chat keys
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
                    String[] quickMsgs = {"Nice!", "Good luck!", "Too slow!", "GG!", "I'm the best!"};
                    int idx = key - KeyEvent.VK_1;
                    if (idx < quickMsgs.length) {
                        parent.sendMessage("CHAT|" + quickMsgs[idx]);
                    }
                }
                if (key == KeyEvent.VK_ESCAPE) {
                    requestFocusInWindow();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        new Thread(this::gameLoop).start();
    }

    // ---- Public methods ----
    public void addFruit(int id, double x, double y, double vx, double vy, int radius, int type) {
        Fruit f = new Fruit(id, x, y, vx, vy, radius, type);
        fruits.put(id, f);
    }

    public void removeFruit(int id) { fruits.remove(id); }

    public void setLastMessage(String msg) { this.lastMessage = msg; }

    public void setWaitingMessage(String msg) {
        this.waitingMessage = msg;
        if (!msg.isEmpty()) {
            fruits.clear();
        } else {
            setGameActive(true);
        }
        repaint();
    }

    public void updateTime(int seconds) {
        this.timeLeft = seconds;
        if (seconds <= 0) gameActive = false;
        else gameActive = true;
        repaint();
    }

    public void setGameActive(boolean active) {
        this.gameActive = active;
        if (!active) {
            fruits.clear();
            setLastMessage("⏰ TIME'S UP! Click Retry to start again.");
        } else {
            setLastMessage("🍉 New round! Slice fruits!");
        }
        combo = 1;
        bigMessage = "";
        repaint();
    }

    public void updateCombo(int c) { this.combo = c; }

    public void updateScore(int s) { this.score = s; }

    public boolean isGameActive() { return gameActive && waitingMessage.isEmpty(); }

    public void addFloatingText(double x, double y, String text, Color color) {
        floatingTexts.add(new FloatingText(x, y, text, color));
    }

    public void showBigMessage(String msg, Color color) {
        bigMessage = msg;
        bigMessageColor = color;
        bigMessageLife = 60;
    }

    public void onFruitSliced(int fruitId, String who, int combo, int points, String bonusTag) {
        removeFruit(fruitId);
        if (!who.equals(parent.getCurrentUser())) {   // <-- FIXED: use getter
            setLastMessage("🍉 " + who + " sliced a fruit (x" + combo + ")");
        } else {
            updateCombo(combo);
            int x = 150 + new Random().nextInt(150);
            int y = 150 + new Random().nextInt(150);
            addFloatingText(x, y, "+" + points, new Color(255, 215, 0));
            if (!bonusTag.isEmpty()) {
                showBigMessage(bonusTag, new Color(255, 100, 0));
            }
        }
        repaint();
    }

    // ---- Game Loop ----
    private void gameLoop() {
        while (true) {
            fruits.values().forEach(Fruit::update);
            fruits.entrySet().removeIf(e -> !e.getValue().alive);
            floatingTexts.removeIf(ft -> {
                ft.update();
                return ft.isExpired();
            });
            if (bigMessageLife > 0) bigMessageLife--;
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }

    // ---- Painting ----
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        GradientPaint gp = new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Floor lines (decorative)
        g2d.setColor(new Color(255, 255, 255, 15));
        for (int i = 0; i < 10; i++) {
            int y = getHeight() - (i * 40);
            g2d.drawLine(0, y, getWidth(), y);
        }

        // Waiting message
        if (!waitingMessage.isEmpty()) {
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(waitingMessage)) / 2;
            int y = getHeight() / 2 - 20;
            g2d.drawString(waitingMessage, x, y);
            g2d.setColor(new Color(255, 200, 50, 100));
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            String sub = "Waiting for other players...";
            fm = g2d.getFontMetrics();
            x = (getWidth() - fm.stringWidth(sub)) / 2;
            g2d.drawString(sub, x, y + 45);
            return;
        }

        // Slice trail
        drawTrail(g2d);

        // Fruits
        for (Fruit f : fruits.values()) {
            drawFruit(g2d, f);
        }

        // Floating texts
        for (FloatingText ft : floatingTexts) {
            ft.draw(g2d);
        }

        // Big message (Combo/Frenzy)
        if (bigMessageLife > 0 && !bigMessage.isEmpty()) {
            float alpha = Math.min(1f, bigMessageLife / 30f);
            g2d.setColor(new Color(bigMessageColor.getRed(), bigMessageColor.getGreen(),
                    bigMessageColor.getBlue(), (int)(alpha * 255)));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(bigMessage)) / 2;
            int y = getHeight() / 2 - 50;
            g2d.drawString(bigMessage, x, y);
        }

        // HUD
        drawHUD(g2d);

        // Bottom message
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int msgX = (getWidth() - fm.stringWidth(lastMessage)) / 2;
        g2d.drawString(lastMessage, msgX, getHeight() - 30);

        // Controls hint
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2d.drawString("Drag to slice | 1-5: Chat | ESC: Focus", 10, getHeight() - 10);
    }

    private void drawTrail(Graphics2D g2d) {
        if (trailPoints.size() > 1) {
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < trailPoints.size() - 1; i++) {
                Point p1 = trailPoints.get(i);
                Point p2 = trailPoints.get(i + 1);
                float alpha = (float) i / trailPoints.size();
                g2d.setColor(new Color(100, 200, 255, (int) (alpha * 150)));
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            g2d.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 255, 255, 30));
            Point first = trailPoints.get(0);
            Point last = trailPoints.get(trailPoints.size() - 1);
            g2d.drawLine(first.x, first.y, last.x, last.y);
        }
    }

    private void drawFruit(Graphics2D g2d, Fruit f) {
        int x = (int) f.x;
        int y = (int) f.y;
        int r = f.radius;
        int type = f.type;
        Color outer, inner, highlight;

        switch (type) {
            case 0 -> { outer = new Color(34, 139, 34); inner = new Color(220, 50, 50); highlight = new Color(255, 100, 100); }
            case 1 -> { outer = new Color(255, 140, 0); inner = new Color(255, 200, 50); highlight = new Color(255, 230, 150); }
            case 2 -> { outer = new Color(180, 30, 30); inner = new Color(220, 60, 60); highlight = new Color(255, 120, 120); }
            default -> { outer = new Color(60, 160, 60); inner = new Color(100, 200, 100); highlight = new Color(180, 255, 180); }
        }

        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x - r + 5, y - r + 8, r * 2, r * 2);

        float centerX = x - r * 0.3f;
        float centerY = y - r * 0.3f;
        RadialGradientPaint rgp = new RadialGradientPaint(
                centerX, centerY, r * 1.5f,
                new float[]{0f, 0.7f, 1f},
                new Color[]{highlight, inner, outer}
        );
        g2d.setPaint(rgp);
        g2d.fillOval(x - r, y - r, r * 2, r * 2);

        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);

        if (type == 0) {
            g2d.setColor(new Color(20, 20, 20));
            g2d.fillOval(x - 8, y - 10, 4, 6);
            g2d.fillOval(x + 6, y - 8, 4, 6);
            g2d.fillOval(x - 4, y + 6, 4, 6);
        }

        g2d.setColor(new Color(80, 60, 30));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x + 2, y - r, x + 8, y - r - 12);
    }

    private void drawHUD(Graphics2D g2d) {
        // Timer
        String timerText = "⏳ " + timeLeft + "s";
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        int timerX = getWidth() - fm.stringWidth(timerText) - 25;
        int timerY = 35;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(timerX - 15, 5, fm.stringWidth(timerText) + 30, 40, 20, 20);
        g2d.setColor(timeLeft <= 5 ? new Color(255, 80, 80) : new Color(255, 215, 0));
        g2d.drawString(timerText, timerX, timerY);

        // Score
        String scoreText = "⚔️ " + score;
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
        fm = g2d.getFontMetrics();
        int scoreX = 15;
        int scoreY = 35;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(scoreX - 10, 5, fm.stringWidth(scoreText) + 30, 40, 20, 20);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString(scoreText, scoreX + 5, scoreY);

        // Combo
        if (combo > 1) {
            String comboText = "🔥 x" + combo;
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
            fm = g2d.getFontMetrics();
            int cx = getWidth() / 2 - fm.stringWidth(comboText) / 2;
            int cy = 30;
            g2d.setColor(new Color(255, 100, 0, 150));
            g2d.fillRoundRect(cx - 10, 5, fm.stringWidth(comboText) + 20, 35, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.drawString(comboText, cx, cy);
        }

        // Status
        g2d.setColor(gameActive ? new Color(0, 255, 0, 80) : new Color(255, 0, 0, 80));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2d.drawString(gameActive ? "🟢 LIVE" : "🔴 FINISHED", 15, 65);
    }

    // ---- Inner classes ----
    private static class Fruit {
        public final int id;
        public double x, y, vx, vy;
        public final int radius;
        public final int type;
        public boolean alive = true;

        public Fruit(int id, double x, double y, double vx, double vy, int radius, int type) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.type = type;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1;
            if (y > 650 || x < -50 || x > 650 || y < -50) alive = false;
        }

        public boolean contains(Point p) {
            return Point.distance(x, y, p.x, p.y) < radius;
        }
    }

    private static class FloatingText {
        private double x, y;
        private final String text;
        private final Color color;
        private int life = 60;
        private float alpha = 1.0f;

        public FloatingText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        public void update() {
            y -= 1.5;
            life--;
            alpha = life / 60.0f;
        }

        public boolean isExpired() { return life <= 0; }

        public void draw(Graphics2D g2d) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (int) (this.x - fm.stringWidth(text) / 2);
            int y = (int) this.y;

            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.drawString(text, x-2, y-2);
            g2d.drawString(text, x+2, y+2);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.drawString(text, x, y);
        }
    }
}