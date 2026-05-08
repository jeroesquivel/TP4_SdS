package ar.edu.itba.sds.sistema2;

import java.util.List;

public final class EnergyTracker {
    public static double kinetic(List<Particle> ps) {
        double e = 0.0;
        for (Particle p : ps) {
            e += 0.5 * p.mass * p.vel.normSq();
        }
        return e;
    }
}
