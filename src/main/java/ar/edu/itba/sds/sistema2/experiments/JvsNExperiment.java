package ar.edu.itba.sds.sistema2.experiments;

import ar.edu.itba.sds.common.DeterministicRandom;
import ar.edu.itba.sds.common.Parallelism;
import ar.edu.itba.sds.common.Stopwatch;
import ar.edu.itba.sds.sistema2.physics.CellIndexMethod;
import ar.edu.itba.sds.sistema2.core.ConfigSeeder;
import ar.edu.itba.sds.sistema2.physics.EnergyTracker;
import ar.edu.itba.sds.sistema2.physics.ForceModel;
import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.sim.Simulator2D;
import ar.edu.itba.sds.sistema2.integrators.VelocityVerletIntegrator2D;
import ar.edu.itba.sds.sistema2.io.CsvWriter;
import ar.edu.itba.sds.sistema2.observables.CfcTracker;
import ar.edu.itba.sds.sistema2.observables.RadialProfileAccumulator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public final class JvsNExperiment {
    private JvsNExperiment() {}

    /** Stats radiales per-bin agregadas sobre realizaciones (mean ± std). */
    public static final class RadialStats {
        public final double[] sCenter;
        public final double[] rhoMean, rhoStd;
        public final double[] vMean, vStd;
        public final double[] jInMean, jInStd;
        public final long[] nSamples;
        public RadialStats(int nBins) {
            sCenter = new double[nBins];
            rhoMean = new double[nBins]; rhoStd = new double[nBins];
            vMean = new double[nBins]; vStd = new double[nBins];
            jInMean = new double[nBins]; jInStd = new double[nBins];
            nSamples = new long[nBins];
        }
        public int nBins() { return sCenter.length; }
    }

    public record Result(int N, double jMean, double jStd, RadialStats radial) {}

    public static Result runForN(int N, int realizations, double k, double tf, double dt, double dt2,
                                 long baseSeed, Path outDir, boolean writePerRealization,
                                 double radialTMin) throws IOException {
        int totalSteps = (int) Math.round(tf / dt);
        int sampleEverySteps = Math.max(1, (int) Math.round(dt2 / dt));
        int snapshotEvery = sampleEverySteps;
        int energyEvery = Math.max(1, totalSteps / 1000);
        int cfcCsvEvery = Math.max(1, totalSteps / 5000);

        double[] jValues = new double[realizations];
        // Matrices per-bin × realization para calcular std al final.
        int nBinsTmp = new RadialProfileAccumulator().nBins();
        final int nBins = nBinsTmp;
        final double[][] rhoPerReal = new double[realizations][nBins];
        final double[][] vPerReal = new double[realizations][nBins];
        final double[][] jInPerReal = new double[realizations][nBins];
        final long[][] samplesPerReal = new long[realizations][nBins];
        AtomicInteger done = new AtomicInteger(0);
        Stopwatch swAll = new Stopwatch().start();

        // Realizaciones independientes en paralelo sobre el pool dedicado.
        try {
            Parallelism.pool().submit(() ->
                IntStream.range(0, realizations).parallel().forEach(r -> {
            try {
                long seed = DeterministicRandom.seedFor(baseSeed, N, r);
                List<Particle> ps = ConfigSeeder.seed(N, seed);
                CellIndexMethod cim = new CellIndexMethod(N, 2.0 * Geometry.R_PARTICLE);
                ForceModel fm = new ForceModel(k, cim);
                VelocityVerletIntegrator2D it = new VelocityVerletIntegrator2D(ps, fm);
                Simulator2D sim = new Simulator2D(ps, fm, it, dt, tf);

                CfcTracker cfc = new CfcTracker(totalSteps);
                RadialProfileAccumulator localRadial = new RadialProfileAccumulator();

                Path cfcCsv = outDir.resolve(String.format("cfc_N%d_real%d.csv", N, r));
                Path enCsv = outDir.resolve(String.format("energy_N%d_real%d.csv", N, r));
                BufferedWriter cfcW = writePerRealization ? CsvWriter.open(cfcCsv) : null;
                BufferedWriter enW = writePerRealization ? CsvWriter.open(enCsv) : null;
                if (cfcW != null) CsvWriter.writeLine(cfcW, "t,cfc");
                if (enW != null) CsvWriter.writeLine(enW, "t,e_kin,e_pot,e_total");

                final BufferedWriter cfcWf = cfcW;
                final BufferedWriter enWf = enW;
                Stopwatch swReal = new Stopwatch().start();
                sim.run((step, t, particles, fmh) -> {
                    cfc.step(particles);
                    try {
                        if (cfcWf != null && (step % cfcCsvEvery == 0 || step == totalSteps)) {
                            CsvWriter.writeLine(cfcWf, String.format("%.6e,%d", t, cfc.cfcPerStep()[step]));
                        }
                        if (enWf != null && step % energyEvery == 0) {
                            double ek = EnergyTracker.kinetic(particles);
                            double ep = fmh.potentialEnergy();
                            CsvWriter.writeLine(enWf, String.format("%.6e,%.6e,%.6e,%.6e", t, ek, ep, ek + ep));
                        }
                        if (step % snapshotEvery == 0 && t >= radialTMin) {
                            localRadial.snapshot(particles);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                swReal.stop();

                if (cfcW != null) cfcW.close();
                if (enW != null) enW.close();

                jValues[r] = computeJ(cfc, dt, tf);
                int finalCfc = cfc.cfcPerStep()[totalSteps];
                // Capturar el perfil radial de esta realización.
                for (int b = 0; b < nBins; b++) {
                    double rho = localRadial.rho(b);
                    double v = localRadial.vMean(b);
                    double jin = localRadial.jIn(b);
                    rhoPerReal[r][b] = rho;
                    vPerReal[r][b] = Double.isNaN(v) ? 0.0 : v;
                    jInPerReal[r][b] = jin;
                    samplesPerReal[r][b] = localRadial.samples(b);
                }
                int d = done.incrementAndGet();
                System.out.printf("[jvsn]     real %2d/%d  J=%.4f  Cfc(tf)=%d  (%.2fs)  [%d/%d done]%n",
                        r + 1, realizations, jValues[r], finalCfc,
                        swReal.elapsedSeconds(), d, realizations);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })
            ).get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        swAll.stop();
        double jMean = mean(jValues);
        double jStd = std(jValues, jMean);

        // Agregar perfiles radiales: mean y std a través de realizaciones,
        // ignorando bins vacíos para v.
        RadialStats stats = new RadialStats(nBins);
        RadialProfileAccumulator template = new RadialProfileAccumulator();
        for (int b = 0; b < nBins; b++) {
            stats.sCenter[b] = template.sCenter(b);
            double sumRho = 0.0, sumJin = 0.0;
            double sumRho2 = 0.0, sumJin2 = 0.0;
            int nV = 0;
            double sumV = 0.0, sumV2 = 0.0;
            long sumSamples = 0;
            for (int r = 0; r < realizations; r++) {
                sumRho += rhoPerReal[r][b];
                sumRho2 += rhoPerReal[r][b] * rhoPerReal[r][b];
                sumJin += jInPerReal[r][b];
                sumJin2 += jInPerReal[r][b] * jInPerReal[r][b];
                sumSamples += samplesPerReal[r][b];
                if (samplesPerReal[r][b] > 0) {
                    sumV += vPerReal[r][b];
                    sumV2 += vPerReal[r][b] * vPerReal[r][b];
                    nV++;
                }
            }
            stats.rhoMean[b] = sumRho / realizations;
            stats.jInMean[b] = sumJin / realizations;
            stats.nSamples[b] = sumSamples;
            stats.rhoStd[b] = realizations < 2 ? 0.0 :
                    Math.sqrt(Math.max(0.0, sumRho2 / realizations - stats.rhoMean[b] * stats.rhoMean[b]) * realizations / (realizations - 1));
            stats.jInStd[b] = realizations < 2 ? 0.0 :
                    Math.sqrt(Math.max(0.0, sumJin2 / realizations - stats.jInMean[b] * stats.jInMean[b]) * realizations / (realizations - 1));
            if (nV > 0) {
                stats.vMean[b] = sumV / nV;
                stats.vStd[b] = nV < 2 ? 0.0 :
                        Math.sqrt(Math.max(0.0, sumV2 / nV - stats.vMean[b] * stats.vMean[b]) * nV / (nV - 1));
            } else {
                stats.vMean[b] = 0.0;
                stats.vStd[b] = 0.0;
            }
        }

        System.out.printf("[jvsn]   N=%d   ⟨J⟩=%.4f ± %.4f   (M=%d en paralelo: %.1fs)%n",
                N, jMean, jStd, realizations, swAll.elapsedSeconds());
        return new Result(N, jMean, jStd, stats);
    }

    public static void runSweep(int[] Ns, int realizations, double k, double tf, double dt, double dt2,
                                long baseSeed, Path outDir, boolean append, double radialTMin) throws IOException {
        Path jCsv = outDir.resolve("j_vs_n.csv");
        boolean writeHeader = !append || !Files.exists(jCsv);
        Stopwatch swTotal = new Stopwatch().start();
        try (BufferedWriter w = append ? CsvWriter.openAppend(jCsv) : CsvWriter.open(jCsv)) {
            if (writeHeader) CsvWriter.writeLine(w, "N,J_mean,J_std,k,realizations,tf");
            int totalSteps = (int) Math.round(tf / dt);
            System.out.printf("[jvsn] dt=%.3e tf=%.0fs k=%.0e  → %d pasos por realización%n",
                    dt, tf, k, totalSteps);
            for (int idx = 0; idx < Ns.length; idx++) {
                int N = Ns[idx];
                Stopwatch swN = new Stopwatch().start();
                System.out.printf("[jvsn] %d/%d  N=%d  M=%d%n", idx + 1, Ns.length, N, realizations);
                Result res = runForN(N, realizations, k, tf, dt, dt2, baseSeed, outDir, true, radialTMin);
                swN.stop();
                CsvWriter.writeLine(w, String.format("%d,%.6e,%.6e,%.3e,%d,%.3f",
                        res.N, res.jMean, res.jStd, k, realizations, tf));
                w.flush();
                writeRadial(res, outDir);
                System.out.printf("[jvsn]   N=%d wall=%.1fs%n", N, swN.elapsedSeconds());
            }
        }
        swTotal.stop();
        System.out.printf("[jvsn] Total: %.1f s%n", swTotal.elapsedSeconds());
    }

    private static void writeRadial(Result res, Path outDir) throws IOException {
        Path csv = outDir.resolve(String.format("radial_N%d.csv", res.N));
        try (BufferedWriter w = CsvWriter.open(csv)) {
            CsvWriter.writeLine(w, "S,rho,rho_std,v_in,v_in_std,j_in,j_in_std,n_samples");
            for (int b = 0; b < res.radial.nBins(); b++) {
                CsvWriter.writeLine(w, String.format("%.4f,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%d",
                        res.radial.sCenter[b],
                        res.radial.rhoMean[b], res.radial.rhoStd[b],
                        res.radial.vMean[b], res.radial.vStd[b],
                        res.radial.jInMean[b], res.radial.jInStd[b],
                        res.radial.nSamples[b]));
            }
        }
    }

    public static double computeJ(CfcTracker cfc, double dt, double tf) {
        int[] cfcArr = cfc.cfcPerStep();
        int total = cfcArr.length - 1;
        int from = 0;                 // ventana completa [0, tf]
        int n = total - from + 1;
        double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumXX = 0.0;
        for (int i = from; i <= total; i++) {
            double t = i * dt;
            double y = cfcArr[i];
            sumX += t;
            sumY += y;
            sumXY += t * y;
            sumXX += t * t;
        }
        double meanX = sumX / n;
        double meanY = sumY / n;
        double num = sumXY - n * meanX * meanY;
        double den = sumXX - n * meanX * meanX;
        if (den == 0.0) return 0.0;
        return num / den;
    }

    private static double mean(double[] x) {
        double s = 0.0;
        for (double v : x) s += v;
        return s / x.length;
    }

    private static double std(double[] x, double m) {
        if (x.length < 2) return 0.0;
        double s = 0.0;
        for (double v : x) s += (v - m) * (v - m);
        return Math.sqrt(s / (x.length - 1));
    }
}
