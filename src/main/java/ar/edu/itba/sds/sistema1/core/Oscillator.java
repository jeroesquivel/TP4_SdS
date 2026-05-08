package ar.edu.itba.sds.sistema1.core;

public final class Oscillator {
    public static final double M = 70.0;
    public static final double K = 1.0e4;
    public static final double GAMMA = 100.0;
    public static final double A = 1.0;
    public static final double T_F = 5.0;

    public static final double R0 = 1.0;
    public static final double V0 = -A * GAMMA / (2.0 * M);

    public static final double OMEGA = Math.sqrt(K / M - (GAMMA * GAMMA) / (4.0 * M * M));
    public static final double DECAY = GAMMA / (2.0 * M);

    private Oscillator() {}

    public static double force(double r, double v) {
        return -K * r - GAMMA * v;
    }

    public static double accel(double r, double v) {
        return force(r, v) / M;
    }

    public static double analyticalR(double t) {
        return A * Math.exp(-DECAY * t) * Math.cos(OMEGA * t);
    }

    public static double analyticalV(double t) {
        double e = Math.exp(-DECAY * t);
        return -A * e * (DECAY * Math.cos(OMEGA * t) + OMEGA * Math.sin(OMEGA * t));
    }
}
