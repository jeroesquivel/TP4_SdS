package ar.edu.itba.sds.sistema2.core;

public final class Particle {
    public static final byte FRESH = 0;
    public static final byte USED = 1;

    public final int id;
    public final double radius;
    public final double mass;
    public final Vec2 pos;
    public final Vec2 vel;
    public final Vec2 acc;
    public final Vec2 accPrev;

    public byte state = FRESH;
    public boolean inContactObs = false;
    public boolean inContactBorder = false;
    public boolean usedReachedBorder = false;

    public Particle(int id, double radius, double mass, double x, double y, double vx, double vy) {
        this.id = id;
        this.radius = radius;
        this.mass = mass;
        this.pos = new Vec2(x, y);
        this.vel = new Vec2(vx, vy);
        this.acc = new Vec2();
        this.accPrev = new Vec2();
    }

    public double radialDot() {
        return pos.x * vel.x + pos.y * vel.y;
    }

    public double radialDistance() {
        return pos.norm();
    }
}
