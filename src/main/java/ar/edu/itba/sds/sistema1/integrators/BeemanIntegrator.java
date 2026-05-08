package ar.edu.itba.sds.sistema1.integrators;

import ar.edu.itba.sds.sistema1.Oscillator;

public final class BeemanIntegrator implements Integrator {
    private final double dt;
    private double r;
    private double v;
    private double a;
    private double aPrev;

    public BeemanIntegrator(double r0, double v0, double dt) {
        this.dt = dt;
        this.r = r0;
        this.v = v0;
        this.a = Oscillator.accel(r0, v0);
        double rPrev = r0 - dt * v0 + 0.5 * dt * dt * a;
        double vPrev = v0 - dt * a;
        this.aPrev = Oscillator.accel(rPrev, vPrev);
    }

    @Override public String name() { return "beeman"; }

    @Override public void step() {
        double rNext = r + v * dt + (2.0 / 3.0) * a * dt * dt - (1.0 / 6.0) * aPrev * dt * dt;
        double vPred = v + (3.0 / 2.0) * a * dt - (1.0 / 2.0) * aPrev * dt;
        double aNext = Oscillator.accel(rNext, vPred);
        double vCorr = v + (1.0 / 3.0) * aNext * dt + (5.0 / 6.0) * a * dt - (1.0 / 6.0) * aPrev * dt;
        aPrev = a;
        a = aNext;
        v = vCorr;
        r = rNext;
    }

    @Override public double r() { return r; }
    @Override public double v() { return v; }
}
