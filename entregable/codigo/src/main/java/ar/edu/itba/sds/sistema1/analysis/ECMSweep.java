package ar.edu.itba.sds.sistema1.analysis;

import ar.edu.itba.sds.sistema1.core.Oscillator;
import ar.edu.itba.sds.sistema1.sim.Simulator;
import ar.edu.itba.sds.sistema1.core.Trajectory;
import ar.edu.itba.sds.sistema1.integrators.BeemanIntegrator;
import ar.edu.itba.sds.sistema1.integrators.EulerIntegrator;
import ar.edu.itba.sds.sistema1.integrators.GearPC5Integrator;
import ar.edu.itba.sds.sistema1.integrators.Integrator;
import ar.edu.itba.sds.sistema1.integrators.VerletIntegrator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ECMSweep {
    public static final double[] DTS = {1.0e-2, 1.0e-3, 1.0e-4, 1.0e-5, 1.0e-6};

    public record Entry(String integrator, Function<Double, Integrator> factory) {}

    public static List<Entry> integrators() {
        return List.of(
                new Entry("euler", dt -> new EulerIntegrator(Oscillator.R0, Oscillator.V0, dt)),
                new Entry("verlet", dt -> new VerletIntegrator(Oscillator.R0, Oscillator.V0, dt)),
                new Entry("beeman", dt -> new BeemanIntegrator(Oscillator.R0, Oscillator.V0, dt)),
                new Entry("gear5", dt -> new GearPC5Integrator(Oscillator.R0, Oscillator.V0, dt))
        );
    }

    public static void writeSweep(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("ecm_sweep.csv");

        record Job(Entry e, double dt) {}
        record Row(String integrator, double dt, double ecm, int totalSteps) {}

        List<Job> jobs = new ArrayList<>();
        for (Entry e : integrators())
            for (double dt : DTS) jobs.add(new Job(e, dt));

        List<Row> rows = jobs.parallelStream().map(job -> {
            Integrator it = job.e.factory.apply(job.dt);
            int totalSteps = (int) Math.round(Oscillator.T_F / job.dt);
            double ecm = Simulator.computeECM(it, job.dt, Oscillator.T_F);
            return new Row(job.e.integrator, job.dt, ecm, totalSteps);
        }).toList();

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv))) {
            pw.println("integrator,dt,ecm,rmse,n_steps");
            for (Row r : rows) {
                double rmse = Math.sqrt(r.ecm);
                pw.printf("%s,%.6e,%.10e,%.10e,%d%n", r.integrator, r.dt, r.ecm, rmse, r.totalSteps);
            }
        }
    }

    public static void writeReferenceTrajectories(Path outDir, double dtRef) throws IOException {
        Files.createDirectories(outDir);
        // Una trayectoria por integrador — independientes, paralelizables.
        integrators().parallelStream().forEach(e -> {
            try {
                Integrator it = e.factory.apply(dtRef);
                int sampleEvery = Math.max(1, (int) Math.round(1.0e-3 / dtRef));
                Trajectory traj = Simulator.run(it, dtRef, Oscillator.T_F, sampleEvery);
                Path csv = outDir.resolve(String.format("trajectory_%s_dt%.0e.csv", e.integrator, dtRef));
                try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv))) {
                    pw.println("t,r_num,r_ana,v_num,v_ana");
                    for (int i = 0; i < traj.t().length; i++) {
                        double t = traj.t()[i];
                        pw.printf("%.6e,%.10e,%.10e,%.10e,%.10e%n",
                                t, traj.r()[i], Oscillator.analyticalR(t), traj.v()[i], Oscillator.analyticalV(t));
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
