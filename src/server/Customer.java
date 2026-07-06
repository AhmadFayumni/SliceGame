package server;

public class Customer {
    public final int id;
    public double x, y;
    public double vx, vy;
    public int radius = 30;
    public int requestedFruit; // 0=apple, 1=orange, 2=banana, 3=watermelon
    public boolean alive = true;
    public long spawnTime;
    private static final long LIFETIME_MS = 5000;

    public Customer(int id, double startX, double startY, int requestedFruit) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.requestedFruit = requestedFruit;
        // Walk horizontally (left or right)
        this.vx = (Math.random() > 0.5) ? 1.5 + Math.random() * 1.5 : -(1.5 + Math.random() * 1.5);
        this.vy = 0;
        this.spawnTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - spawnTime) > LIFETIME_MS;
    }

    public void update() {
        x += vx;
        // Bounce off walls or despawn
        if (x > 570 || x < 30) {
            alive = false;
        }
    }
}