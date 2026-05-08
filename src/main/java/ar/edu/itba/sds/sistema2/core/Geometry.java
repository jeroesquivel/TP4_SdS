package ar.edu.itba.sds.sistema2.core;

public final class Geometry {
    public static final double L = 80.0;
    public static final double R = L / 2.0;
    public static final double R_OBSTACLE = 1.0;
    public static final double R_PARTICLE = 1.0;
    public static final double MASS = 1.0;
    public static final double V0 = 1.0;

    public static double dtForK(double k) {
        double tau = 2.0 * Math.PI * Math.sqrt(MASS / k);
        return tau / 100.0;
    }

    private Geometry() {}
}
