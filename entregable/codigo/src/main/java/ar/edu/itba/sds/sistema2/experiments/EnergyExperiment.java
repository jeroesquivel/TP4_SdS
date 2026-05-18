package ar.edu.itba.sds.sistema2.experiments;

import ar.edu.itba.sds.common.DeterministicRandom;
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

public final class EnergyExperiment {
    private EnergyExperiment() {}

    public static void run(int N, double k, double tf, long seed, String integratorName, Path outDir)
            throws IOException {
        double dt = Geometry.dtForK(k);
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

        Path csv = outDir.resolve(String.format("energy_%s_N%d_k%.0e.csv", it.name(), N, k));
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
            });
        }
        System.out.printf("[energy] %s N=%d k=%.0e dt=%.3e tf=%.1f → %s%n",
                it.name(), N, k, dt, tf, csv);
    }
}
