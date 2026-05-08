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
import java.nio.file.Path;
import java.util.List;

public final class KSweepExperiment {
    private KSweepExperiment() {}

    private static final double S_NEAR_LO = 1.5;
    private static final double S_NEAR_HI = 3.0;

    public static void run(double[] ks, int[] Ns, int realizations, double tf, double dt2,
                           long baseSeed, Path outDir) throws IOException {
        Path csv = outDir.resolve("k_sweep.csv");
        Stopwatch swTotal = new Stopwatch().start();
        try (BufferedWriter w = CsvWriter.open(csv)) {
            CsvWriter.writeLine(w, "k,N,J_mean,J_std,J_in_S2_mean,realizations,tf,dt");
            int totalCombinations = ks.length * Ns.length;
            int doneCombinations = 0;
            for (double k : ks) {
                double dt = Geometry.dtForK(k);
                int totalSteps = (int) Math.round(tf / dt);
                Stopwatch swK = new Stopwatch().start();
                System.out.printf("[ksweep] k=%.0e  dt=%.3e tf=%.0fs → %d pasos/realización%n",
                        k, dt, tf, totalSteps);
                for (int N : Ns) {
                    doneCombinations++;
                    Stopwatch swN = new Stopwatch().start();
                    System.out.printf("[ksweep] [%d/%d] k=%.0e N=%d M=%d ...%n",
                            doneCombinations, totalCombinations, k, N, realizations);
                    double[] jValues = new double[realizations];
                    RadialProfileAccumulator radial = new RadialProfileAccumulator();
                    int snapshotEvery = Math.max(1, (int) Math.round(dt2 / dt));

                    int energyEvery = Math.max(1, totalSteps / 1000);
                    for (int r = 0; r < realizations; r++) {
                        Stopwatch swReal = new Stopwatch().start();
                        long seed = DeterministicRandom.seedFor(baseSeed + (long) Math.round(Math.log10(k)) * 1_000_000L,
                                N, r);
                        List<Particle> ps = ConfigSeeder.seed(N, seed);
                        CellIndexMethod cim = new CellIndexMethod(N, 2.0 * Geometry.R_PARTICLE);
                        ForceModel fm = new ForceModel(k, cim);
                        VelocityVerletIntegrator2D it = new VelocityVerletIntegrator2D(ps, fm);
                        Simulator2D sim = new Simulator2D(ps, fm, it, dt, tf);
                        CfcTracker cfc = new CfcTracker(totalSteps);

                        boolean writeEnergy = (r == 0);
                        Path enCsv = outDir.resolve(String.format("energy_k%.0e_N%d.csv", k, N));
                        BufferedWriter enW = writeEnergy ? CsvWriter.open(enCsv) : null;
                        if (enW != null) CsvWriter.writeLine(enW, "t,e_kin,e_pot,e_total");

                        sim.run((step, t, particles, fmh) -> {
                            cfc.step(particles);
                            try {
                                if (enW != null && step % energyEvery == 0) {
                                    double ek = EnergyTracker.kinetic(particles);
                                    double ep = fmh.potentialEnergy();
                                    CsvWriter.writeLine(enW, String.format("%.6e,%.6e,%.6e,%.6e",
                                            t, ek, ep, ek + ep));
                                }
                                if (step % snapshotEvery == 0 && t >= tf / 2.0) {
                                    radial.snapshot(particles);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        if (enW != null) enW.close();

                        jValues[r] = JvsNExperiment.computeJ(cfc, dt, tf);
                        swReal.stop();
                        System.out.printf("[ksweep]     real %2d/%d  J=%.4f  (%.1fs)%n",
                                r + 1, realizations, jValues[r], swReal.elapsedSeconds());
                    }

                    double jMean = mean(jValues);
                    double jStd = std(jValues, jMean);
                    double jInNear = jInNearAverage(radial);

                    CsvWriter.writeLine(w, String.format("%.3e,%d,%.6e,%.6e,%.6e,%d,%.3f,%.3e",
                            k, N, jMean, jStd, jInNear, realizations, tf, dt));
                    w.flush();
                    swN.stop();
                    System.out.printf("[ksweep]   k=%.0e N=%d → ⟨J⟩=%.4f ± %.4f, ⟨J^in|S~2⟩=%.4f  (%.1fs)%n",
                            k, N, jMean, jStd, jInNear, swN.elapsedSeconds());
                }
                swK.stop();
                System.out.printf("[ksweep] subtotal k=%.0e: %.1fs%n", k, swK.elapsedSeconds());
            }
        }
        swTotal.stop();
        System.out.printf("[ksweep] Total: %.1fs%n", swTotal.elapsedSeconds());
    }

    private static double jInNearAverage(RadialProfileAccumulator radial) {
        double sum = 0.0;
        int count = 0;
        for (int b = 0; b < radial.nBins(); b++) {
            double s = radial.sCenter(b);
            if (s >= S_NEAR_LO && s <= S_NEAR_HI) {
                sum += radial.jIn(b);
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
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
