package ar.edu.itba.sds.sistema2.core;

public final class Vec2 {
    public double x;
    public double y;

    public Vec2() {}

    public Vec2(double x, double y) { this.x = x; this.y = y; }

    public double norm() { return Math.sqrt(x * x + y * y); }

    public double normSq() { return x * x + y * y; }

    public void set(Vec2 o) { this.x = o.x; this.y = o.y; }

    public void zero() { x = 0.0; y = 0.0; }
}
