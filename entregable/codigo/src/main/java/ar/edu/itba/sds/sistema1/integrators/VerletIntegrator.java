package ar.edu.itba.sds.sistema1.integrators;

import ar.edu.itba.sds.sistema1.core.Oscillator;

public final class VerletIntegrator implements Integrator {
    private final double dt;
    private double rPrev;
    private double r;
    private double v;
    private double vLag;

    public VerletIntegrator(double r0, double v0, double dt) {
        this.dt = dt;
        this.r = r0;
        this.v = v0;
        this.vLag = v0;
        double a0 = Oscillator.accel(r0, v0);
        this.rPrev = r0 - dt * v0 + 0.5 * dt * dt * a0;
    }

    @Override public String name() { return "verlet"; }

    @Override public void step() {
        double a = Oscillator.accel(r, vLag);
        double rNext = 2.0 * r - rPrev + dt * dt * a;
        double vNew = (rNext - rPrev) / (2.0 * dt);
        rPrev = r;
        r = rNext;
        vLag = vNew;
        v = vNew;
    }

    @Override public double r() { return r; }
    @Override public double v() { return v; }
}
