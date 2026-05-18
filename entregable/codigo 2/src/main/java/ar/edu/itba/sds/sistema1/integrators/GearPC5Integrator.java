package ar.edu.itba.sds.sistema1.integrators;

import ar.edu.itba.sds.sistema1.core.Oscillator;

public final class GearPC5Integrator implements Integrator {
    private static final double[] ALPHA = {3.0 / 16.0, 251.0 / 360.0, 1.0, 11.0 / 18.0, 1.0 / 6.0, 1.0 / 60.0};

    private final double dt;
    private double R0, R1, R2, R3, R4, R5;

    public GearPC5Integrator(double r0, double v0, double dt) {
        this.dt = dt;
        double k = Oscillator.K;
        double m = Oscillator.M;
        double g = Oscillator.GAMMA;

        double r2_0 = (-k * r0 - g * v0) / m;
        double r3_0 = (-k * v0 - g * r2_0) / m;
        double r4_0 = (-k * r2_0 - g * r3_0) / m;
        double r5_0 = (-k * r3_0 - g * r4_0) / m;

        this.R0 = r0;
        this.R1 = v0 * dt;
        this.R2 = r2_0 * dt * dt / 2.0;
        this.R3 = r3_0 * dt * dt * dt / 6.0;
        this.R4 = r4_0 * dt * dt * dt * dt / 24.0;
        this.R5 = r5_0 * dt * dt * dt * dt * dt / 120.0;
    }

    @Override public String name() { return "gear5"; }

    @Override public void step() {
        double R0p = R0 + R1 + R2 + R3 + R4 + R5;
        double R1p = R1 + 2.0 * R2 + 3.0 * R3 + 4.0 * R4 + 5.0 * R5;
        double R2p = R2 + 3.0 * R3 + 6.0 * R4 + 10.0 * R5;
        double R3p = R3 + 4.0 * R4 + 10.0 * R5;
        double R4p = R4 + 5.0 * R5;
        double R5p = R5;

        double rPred = R0p;
        double vPred = R1p / dt;
        double aReal = Oscillator.accel(rPred, vPred);
        double dR2 = aReal * dt * dt / 2.0 - R2p;

        R0 = R0p + ALPHA[0] * dR2;
        R1 = R1p + ALPHA[1] * dR2;
        R2 = R2p + ALPHA[2] * dR2;
        R3 = R3p + ALPHA[3] * dR2;
        R4 = R4p + ALPHA[4] * dR2;
        R5 = R5p + ALPHA[5] * dR2;
    }

    @Override public double r() { return R0; }
    @Override public double v() { return R1 / dt; }
}
