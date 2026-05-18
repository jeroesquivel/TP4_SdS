package ar.edu.itba.sds.sistema2.observables;

import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;

import java.util.List;

public final class RadialProfileAccumulator {
    public static final double DS = 0.2;
    private final double sMin;
    private final double sMax;
    private final int nBins;
    private final long[] count;
    private final double[] sumVRadial;
    private long snapshots;

    public RadialProfileAccumulator() {
        this.sMin = Geometry.R_OBSTACLE + Geometry.R_PARTICLE;
        this.sMax = Geometry.R - Geometry.R_PARTICLE;
        this.nBins = (int) Math.ceil((sMax - sMin) / DS);
        this.count = new long[nBins];
        this.sumVRadial = new double[nBins];
    }

    public void snapshot(List<Particle> particles) {
        snapshots++;
        for (Particle p : particles) {
            if (p.state != Particle.FRESH) continue;
            double dot = p.pos.x * p.vel.x + p.pos.y * p.vel.y;
            if (dot >= 0.0) continue;
            double s = p.pos.norm();
            if (s < sMin || s >= sMax) continue;
            int bin = (int) Math.floor((s - sMin) / DS);
            if (bin < 0 || bin >= nBins) continue;
            double vRadial = dot / s;
            count[bin]++;
            sumVRadial[bin] += vRadial;
        }
    }

    public int nBins() { return nBins; }

    public double sCenter(int bin) { return sMin + (bin + 0.5) * DS; }

    public double rho(int bin) {
        double s = sCenter(bin);
        double area = 2.0 * Math.PI * s * DS;
        return ((double) count[bin] / Math.max(snapshots, 1)) / area;
    }

    public double vMean(int bin) {
        if (count[bin] == 0) return Double.NaN;
        return -sumVRadial[bin] / count[bin];
    }

    public double jIn(int bin) {
        double r = rho(bin);
        double v = vMean(bin);
        if (Double.isNaN(v)) return 0.0;
        return r * Math.abs(v);
    }

    public long samples(int bin) { return count[bin]; }

    /** Merges another accumulator into this one. NOT thread-safe; call from a single thread. */
    public void mergeFrom(RadialProfileAccumulator other) {
        if (other.nBins != this.nBins)
            throw new IllegalArgumentException("nBins mismatch");
        this.snapshots += other.snapshots;
        for (int i = 0; i < nBins; i++) {
            this.count[i] += other.count[i];
            this.sumVRadial[i] += other.sumVRadial[i];
        }
    }
}
