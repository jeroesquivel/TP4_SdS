package ar.edu.itba.sds.common;

import java.util.SplittableRandom;

public final class DeterministicRandom {
    private final SplittableRandom rng;

    public DeterministicRandom(long seed) {
        this.rng = new SplittableRandom(seed);
    }

    public double uniform(double lo, double hi) {
        return lo + (hi - lo) * rng.nextDouble();
    }

    public double nextDouble() {
        return rng.nextDouble();
    }

    public static long seedFor(long baseSeed, int N, int realizationIdx) {
        return baseSeed + 1000L * N + realizationIdx;
    }
}
