package ar.edu.itba.sds.sistema1.integrators;

import ar.edu.itba.sds.sistema1.core.Oscillator;

public final class EulerIntegrator implements Integrator {
    private final double dt;
    private double r;
    private double v;

    public EulerIntegrator(double r0, double v0, double dt) {
        this.r = r0;
        this.v = v0;
        this.dt = dt;
    }

    @Override public String name() { return "euler"; }

    @Override public void step() {
        double a = Oscillator.accel(r, v);
        double vPred = v + dt * a;
        double rPred = r + dt * v;
        double aNew = Oscillator.accel(rPred, vPred);
        double vNew = v + dt * aNew;
        double rNew = r + dt * vNew;
        r = rNew;
        v = vNew;
    }

    @Override public double r() { return r; }
    @Override public double v() { return v; }
}
