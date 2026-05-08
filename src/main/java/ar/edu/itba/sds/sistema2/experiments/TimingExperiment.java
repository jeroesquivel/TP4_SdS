package ar.edu.itba.sds.sistema2.experiments;

import ar.edu.itba.sds.common.DeterministicRandom;
import ar.edu.itba.sds.common.Stopwatch;
import ar.edu.itba.sds.sistema2.physics.CellIndexMethod;
import ar.edu.itba.sds.sistema2.core.ConfigSeeder;
import ar.edu.itba.sds.sistema2.physics.ForceModel;
import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.sim.Simulator2D;
import ar.edu.itba.sds.sistema2.integrators.VelocityVerletIntegrator2D;
import ar.edu.itba.sds.sistema2.io.CsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class TimingExperiment {
    private TimingExperiment() {}

    public static void run(int[] Ns, double k, double tf, long baseSeed, Path outDir) throws IOException {
        Path csv = outDir.resolve("timing.csv");
        try (BufferedWriter w = CsvWriter.open(csv)) {
            CsvWriter.writeLine(w, "N,t_exec_s,dt,tf,k");
            double dt = Geometry.dtForK(k);
            int totalSteps = (int) Math.round(tf / dt);
            System.out.printf("[timing] dt=%.3e tf=%.0fs k=%.0e  → %d pasos por corrida%n",
                    dt, tf, k, totalSteps);
            for (int N : Ns) {
                long seed = DeterministicRandom.seedFor(baseSeed, N, 0);
                System.out.printf("[timing]   N=%d → seeding ... ", N);
                System.out.flush();
                Stopwatch swSeed = new Stopwatch().start();
                List<Particle> ps = ConfigSeeder.seed(N, seed);
                swSeed.stop();
                System.out.printf("(%.2fs) running ... ", swSeed.elapsedSeconds());
                System.out.flush();

                CellIndexMethod cim = new CellIndexMethod(N, 2.0 * Geometry.R_PARTICLE);
                ForceModel fm = new ForceModel(k, cim);
                VelocityVerletIntegrator2D it = new VelocityVerletIntegrator2D(ps, fm);
                Simulator2D sim = new Simulator2D(ps, fm, it, dt, tf);

                Stopwatch sw = new Stopwatch().start();
                sim.run(null);
                sw.stop();
                double sec = sw.elapsedSeconds();

                System.out.printf("t_exec=%.2fs  (%.2f µs/step)%n", sec, 1.0e6 * sec / totalSteps);
                CsvWriter.writeLine(w, String.format("%d,%.6f,%.6e,%.3f,%.3e", N, sec, dt, tf, k));
                w.flush();
            }
        }
    }
}
