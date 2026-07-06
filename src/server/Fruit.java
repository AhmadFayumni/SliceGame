package server;

public class Fruit {
    public final int id;
    public double x, y;
    public double vx, vy;
    public int radius;
    public int type; // 0=apple,1=orange,2=banana,3=watermelon
    public boolean alive = true;
    public long spawnTime;
    private static final long LIFETIME_MS = 4000;

    public Fruit(int id, double startX, double startY, int radius, double vx, double vy, int type) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.radius = radius;
        this.vx = vx;
        this.vy = vy;
        this.type = type;
        this.spawnTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - spawnTime) > LIFETIME_MS;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1; // gravity
        if (y > 650 || x < -50 || x > 650) alive = false;
    }
}