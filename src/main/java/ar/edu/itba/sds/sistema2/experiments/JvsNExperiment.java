package ar.edu.itba.sds.sistema2.experiments;

import ar.edu.itba.sds.common.DeterministicRandom;
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

public final class JvsNExperiment {
    private JvsNExperiment() {}

    public record Result(int N, double jMean, double jStd, RadialProfileAccumulator radial) {}

    public static Result runForN(int N, int realizations, double k, double tf, double dt, double dt2,
                                 long baseSeed, Path outDir, boolean writePerRealization,
                                 double radialTMin) throws IOException {
        int totalSteps = (int) Math.round(tf / dt);
        int sampleEverySteps = Math.max(1, (int) Math.round(dt2 / dt));
        int snapshotEvery = sampleEverySteps;
        int energyEvery = Math.max(1, totalSteps / 1000);
        int cfcCsvEvery = Math.max(1, totalSteps / 5000);

        double[] jValues = new double[realizations];
        RadialProfileAccumulator radial = new RadialProfileAccumulator();

        for (int r = 0; r < realizations; r++) {
            Stopwatch swReal = new Stopwatch().start();
            long seed = DeterministicRandom.seedFor(baseSeed, N, r);
            List<Particle> ps = ConfigSeeder.seed(N, seed);
            CellIndexMethod cim = new CellIndexMethod(N, 2.0 * Geometry.R_PARTICLE);
            ForceModel fm = new ForceModel(k, cim);
            VelocityVerletIntegrator2D it = new VelocityVerletIntegrator2D(ps, fm);
            Simulator2D sim = new Simulator2D(ps, fm, it, dt, tf);

            CfcTracker cfc = new CfcTracker(totalSteps);

            Path cfcCsv = outDir.resolve(String.format("cfc_N%d_real%d.csv", N, r));
            Path enCsv = outDir.resolve(String.format("energy_N%d_real%d.csv", N, r));
            BufferedWriter cfcW = writePerRealization ? CsvWriter.open(cfcCsv) : null;
            BufferedWriter enW = writePerRealization ? CsvWriter.open(enCsv) : null;
            if (cfcW != null) CsvWriter.writeLine(cfcW, "t,cfc");
            if (enW != null) CsvWriter.writeLine(enW, "t,e_kin,e_pot,e_total");

            sim.run((step, t, particles, fmh) -> {
                cfc.step(particles);
                try {
                    if (cfcW != null && (step % cfcCsvEvery == 0 || step == totalSteps)) {
                        CsvWriter.writeLine(cfcW, String.format("%.6e,%d", t, cfc.cfcPerStep()[step]));
                    }
                    if (enW != null && step % energyEvery == 0) {
                        double ek = EnergyTracker.kinetic(particles);
                        double ep = fmh.potentialEnergy();
                        CsvWriter.writeLine(enW, String.format("%.6e,%.6e,%.6e,%.6e", t, ek, ep, ek + ep));
                    }
                    if (step % snapshotEvery == 0 && t >= radialTMin) {
                        radial.snapshot(particles);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            if (cfcW != null) cfcW.close();
            if (enW != null) enW.close();

            jValues[r] = computeJ(cfc, dt, tf);
            swReal.stop();
            int finalCfc = cfc.cfcPerStep()[totalSteps];
            System.out.printf("[jvsn]     real %2d/%d  J=%.4f  Cfc(tf)=%d  (%.2fs)%n",
                    r + 1, realizations, jValues[r], finalCfc, swReal.elapsedSeconds());
        }

        double jMean = mean(jValues);
        double jStd = std(jValues, jMean);
        return new Result(N, jMean, jStd, radial);
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
                System.out.printf("[jvsn]   ⟨J⟩=%.4f ± %.4f  (N=%d → %.1fs)%n",
                        res.jMean, res.jStd, N, swN.elapsedSeconds());
            }
        }
        swTotal.stop();
        System.out.printf("[jvsn] Total: %.1f s%n", swTotal.elapsedSeconds());
    }

    private static void writeRadial(Result res, Path outDir) throws IOException {
        Path csv = outDir.resolve(String.format("radial_N%d.csv", res.N));
        try (BufferedWriter w = CsvWriter.open(csv)) {
            CsvWriter.writeLine(w, "S,rho,v_in,j_in,n_samples");
            for (int b = 0; b < res.radial.nBins(); b++) {
                CsvWriter.writeLine(w, String.format("%.4f,%.6e,%.6e,%.6e,%d",
                        res.radial.sCenter(b), res.radial.rho(b),
                        Double.isNaN(res.radial.vMean(b)) ? 0.0 : res.radial.vMean(b),
                        res.radial.jIn(b), res.radial.samples(b)));
            }
        }
    }

    public static double computeJ(CfcTracker cfc, double dt, double tf) {
        int[] cfcArr = cfc.cfcPerStep();
        int total = cfcArr.length - 1;
        int from = total / 2;
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
