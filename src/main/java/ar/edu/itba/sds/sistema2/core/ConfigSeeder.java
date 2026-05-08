package ar.edu.itba.sds.sistema2.core;

import ar.edu.itba.sds.common.DeterministicRandom;

import java.util.ArrayList;
import java.util.List;

public final class ConfigSeeder {
    private ConfigSeeder() {}

    public static List<Particle> seed(int N, long seed) {
        DeterministicRandom rng = new DeterministicRandom(seed);
        List<Particle> ps = new ArrayList<>(N);

        double rMax = Geometry.R - Geometry.R_PARTICLE;
        double minSepObs = Geometry.R_OBSTACLE + Geometry.R_PARTICLE;
        double minSepPair = 2.0 * Geometry.R_PARTICLE;

        double availableArea = Math.PI * (rMax * rMax - minSepObs * minSepObs);
        double packingFraction = N * Math.PI * Geometry.R_PARTICLE * Geometry.R_PARTICLE / availableArea;
        if (packingFraction > 0.45) {
            seedFromGrid(N, rng, ps, rMax, minSepObs, minSepPair);
            return ps;
        }

        long maxAttempts = 1000L * N;
        long attempts = 0;

        while (ps.size() < N) {
            if (++attempts > maxAttempts) {
                seedFromGrid(N, rng, ps, rMax, minSepObs, minSepPair);
                return ps;
            }
            double angle = rng.uniform(0.0, 2.0 * Math.PI);
            double rRadial = Math.sqrt(rng.nextDouble()) * rMax;
            double x = rRadial * Math.cos(angle);
            double y = rRadial * Math.sin(angle);

            double dObs = Math.sqrt(x * x + y * y);
            if (dObs < minSepObs) continue;

            boolean ok = true;
            for (Particle p : ps) {
                double dx = x - p.pos.x;
                double dy = y - p.pos.y;
                if (dx * dx + dy * dy < minSepPair * minSepPair) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            double theta = rng.uniform(0.0, 2.0 * Math.PI);
            double vx = Geometry.V0 * Math.cos(theta);
            double vy = Geometry.V0 * Math.sin(theta);
            ps.add(new Particle(ps.size(), Geometry.R_PARTICLE, Geometry.MASS, x, y, vx, vy));
        }
        return ps;
    }

    private static void seedFromGrid(int N, DeterministicRandom rng, List<Particle> ps,
                                     double rMax, double minSepObs, double minSepPair) {
        double cell = minSepPair * 1.05;
        double rowH = cell * Math.sqrt(3.0) / 2.0;
        java.util.List<double[]> slots = new java.util.ArrayList<>();
        int row = 0;
        for (double y = -rMax; y <= rMax; y += rowH, row++) {
            double offset = (row % 2 == 0) ? 0.0 : cell / 2.0;
            for (double x = -rMax + offset; x <= rMax; x += cell) {
                double r = Math.sqrt(x * x + y * y);
                if (r >= minSepObs && r <= rMax) slots.add(new double[]{x, y});
            }
        }
        if (slots.size() < N) {
            throw new IllegalStateException("Grid fallback only fits " + slots.size() + " slots; need " + N);
        }
        for (int i = slots.size() - 1; i > 0; i--) {
            int j = (int) Math.floor(rng.nextDouble() * (i + 1));
            double[] tmp = slots.get(i); slots.set(i, slots.get(j)); slots.set(j, tmp);
        }
        ps.clear();
        for (int i = 0; i < N; i++) {
            double[] s = slots.get(i);
            double theta = rng.uniform(0.0, 2.0 * Math.PI);
            double vx = Geometry.V0 * Math.cos(theta);
            double vy = Geometry.V0 * Math.sin(theta);
            ps.add(new Particle(i, Geometry.R_PARTICLE, Geometry.MASS, s[0], s[1], vx, vy));
        }
    }
}
