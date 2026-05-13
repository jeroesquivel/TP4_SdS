package ar.edu.itba.sds.sistema2.core;

public final class Geometry {
    public static final double L = 80.0;
    public static final double R = L / 2.0;
    public static final double R_OBSTACLE = 1.0;
    public static final double R_PARTICLE = 1.0;
    public static final double MASS = 1.0;
    public static final double V0 = 1.0;
    //si no entra en los default hace esa cuenta falopa, hay que sacarla despues
    public static double dtForK(double k) {
        if (k == 100.0)   return 1.0e-2;
        if (k == 1000.0)  return 1.0e-3;
        if (k == 10000.0) return 1.0e-3;
        double tau = 2.0 * Math.PI * Math.sqrt(MASS / k);
        return tau / 100.0;
    }

    private Geometry() {}
}
