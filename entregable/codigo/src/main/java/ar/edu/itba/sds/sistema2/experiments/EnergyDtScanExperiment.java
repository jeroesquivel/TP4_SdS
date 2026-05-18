package ar.edu.itba.sds.sistema2.experiments;

import ar.edu.itba.sds.sistema2.physics.CellIndexMethod;
import ar.edu.itba.sds.sistema2.core.ConfigSeeder;
import ar.edu.itba.sds.sistema2.physics.EnergyTracker;
import ar.edu.itba.sds.sistema2.physics.ForceModel;
import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.sim.Simulator2D;
import ar.edu.itba.sds.sistema2.integrators.BeemanIntegrator2D;
import ar.edu.itba.sds.sistema2.integrators.Integrator2D;
import ar.edu.itba.sds.sistema2.integrators.VelocityVerletIntegrator2D;
import ar.edu.itba.sds.sistema2.io.CsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public final class EnergyDtScanExperiment {
    private EnergyDtScanExperiment() {}

    public static void runOne(int N, double k, double dt, double tf, long seed,
                              String integratorName, Path outDir) throws IOException {
        runOne(N, k, dt, tf, seed, integratorName, outDir, "");
    }

    public static void runOne(int N, double k, double dt, double tf, long seed,
                              String integratorName, Path outDir, String tag) throws IOException {
        List<Particle> ps = ConfigSeeder.seed(N, seed);
        CellIndexMethod cim = new CellIndexMethod(N, 2.0 * Geometry.R_PARTICLE);
        ForceModel fm = new ForceModel(k, cim);
        Integrator2D it = switch (integratorName) {
            case "beeman" -> new BeemanIntegrator2D(ps, fm);
            default -> new VelocityVerletIntegrator2D(ps, fm);
        };
        Simulator2D sim = new Simulator2D(ps, fm, it, dt, tf);
        int totalSteps = (int) Math.round(tf / dt);
        int every = Math.max(1, totalSteps / 2000);

        String header = String.format("[energy_dt_scan]%s k=%.0e dt=%.0e N=%d tf=%.1f steps=%d",
                tag, k, dt, N, tf, totalSteps);
        System.out.println(header);

        Path csv = outDir.resolve(String.format("energy_dtscan_k%.0e_dt%.0e.csv", k, dt));
        long t0 = System.currentTimeMillis();
        long[] lastPrint = { t0 };
        try (BufferedWriter w = CsvWriter.open(csv)) {
            CsvWriter.writeLine(w, "t,e_kin,e_pot,e_total,dt,k");
            sim.run((step, t, particles, fmh) -> {
                if (step % every == 0) {
                    double ek = EnergyTracker.kinetic(particles);
                    double ep = fmh.potentialEnergy();
                    try {
                        CsvWriter.writeLine(w, String.format("%.6e,%.6e,%.6e,%.6e,%.3e,%.3e",
                                t, ek, ep, ek + ep, dt, k));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                long now = System.currentTimeMillis();
                if (step > 0 && (now - lastPrint[0] >= 500 || step == totalSteps)) {
                    lastPrint[0] = now;
                    double frac = step / (double) totalSteps;
                    long elapsed = (now - t0) / 1000;
                    long eta = (long) (elapsed * (1.0 - frac) / Math.max(frac, 1e-9));
                    int barW = 24;
                    int filled = (int) Math.round(frac * barW);
                    StringBuilder bar = new StringBuilder("[");
                    for (int i = 0; i < barW; i++) bar.append(i < filled ? '=' : (i == filled ? '>' : ' '));
                    bar.append(']');
                    System.out.printf("\r    %s %5.1f%%  t=%6.2f/%.1fs  elapsed=%ds  eta=%ds   ",
                            bar, frac * 100.0, t, tf, elapsed, eta);
                    System.out.flush();
                }
            });
        }
        long totalSec = (System.currentTimeMillis() - t0) / 1000;
        System.out.printf("%n    done in %ds → %s%n", totalSec, csv);
    }

    public static void runSweep(int N, double[] ks, double[] dts, double tf,
                                long seed, String integratorName, Path outDir)
            throws IOException {
        int total = ks.length * dts.length;
        long sweepStart = System.currentTimeMillis();
        AtomicInteger done = new AtomicInteger(0);

        // Cada (k, dt) es independiente — paralelizar a través de tareas.
        record Task(int ki, int di, double k, double dt) {}
        java.util.List<Task> tasks = new java.util.ArrayList<>();
        for (int ki = 0; ki < ks.length; ki++)
            for (int di = 0; di < dts.length; di++)
                tasks.add(new Task(ki, di, ks[ki], dts[di]));

        tasks.parallelStream().forEach(task -> {
            String tag = String.format(" [k=%d/%d dt=%d/%d]",
                    task.ki + 1, ks.length, task.di + 1, dts.length);
            try {
                runOne(N, task.k, task.dt, tf, seed, integratorName, outDir, tag);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int d = done.incrementAndGet();
            System.out.printf("[energy_dt_scan] %d/%d done%n", d, total);
        });

        long totalSec = (System.currentTimeMillis() - sweepStart) / 1000;
        System.out.printf("%n[energy_dt_scan] sweep done: %d corridas en %ds (%02d:%02d:%02d)%n",
                total, totalSec, totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60);
    }
}
