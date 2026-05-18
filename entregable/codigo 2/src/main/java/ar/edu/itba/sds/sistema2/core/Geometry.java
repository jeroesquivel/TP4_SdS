package ar.edu.itba.sds.sistema2.core;

public final class Geometry {
    public static final double L = 80.0;
    public static final double R = L / 2.0;
    public static final double R_OBSTACLE = 1.0;
    public static final double R_PARTICLE = 1.0;
    public static final double MASS = 1.0;
    public static final double V0 = 1.0;
    public static double dtForK(double k) {
        // Δt validados con el barrido energy_dt_scan (tf=2000, N=800):
        // drift máximo de E por debajo del 1% en todos los casos.
        if (k == 100.0)    return 5.0e-3;
        if (k == 1000.0)   return 1.0e-3;
        if (k == 10000.0)  return 1.0e-3;
        if (k == 100000.0) return 5.0e-4;
        // fallback: τ_col/200 para k no listados.
        double tau = 2.0 * Math.PI * Math.sqrt(MASS / k);
        return tau / 200.0;
    }

    private Geometry() {}
}
