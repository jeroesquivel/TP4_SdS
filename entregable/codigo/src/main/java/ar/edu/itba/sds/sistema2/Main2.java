package ar.edu.itba.sds.sistema2;

import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.experiments.EnergyDtScanExperiment;
import ar.edu.itba.sds.sistema2.experiments.EnergyExperiment;
import ar.edu.itba.sds.sistema2.experiments.JvsNExperiment;
import ar.edu.itba.sds.sistema2.experiments.KSweepExperiment;
import ar.edu.itba.sds.sistema2.experiments.TimingExperiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Main2 {
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        ar.edu.itba.sds.common.Parallelism.configure();
        Map<String, String> opts = parseArgs(args);
        String experiment = opts.getOrDefault("experiment", "energy");
        long baseSeed = Long.parseLong(opts.getOrDefault("seed", "42"));
        double tf = Double.parseDouble(opts.getOrDefault("tf", "500"));
        double k = Double.parseDouble(opts.getOrDefault("k", "1000"));
        int realizations = Integer.parseInt(opts.getOrDefault("realizations", "10"));
        boolean append = Boolean.parseBoolean(opts.getOrDefault("append", "false"));
        Path out = Path.of(opts.getOrDefault("outdir", "results/s2"));

        switch (experiment) {
            case "energy" -> {
                int N = Integer.parseInt(opts.getOrDefault("N", "100"));
                double tfShort = Double.parseDouble(opts.getOrDefault("tf", "5"));
                String integ = opts.getOrDefault("integrator", "velocity_verlet");
                EnergyExperiment.run(N, k, tfShort, baseSeed, integ, out);
            }
            case "energy_dt_scan" -> {
                int N = Integer.parseInt(opts.getOrDefault("N", "800"));
                double tfScan = Double.parseDouble(opts.getOrDefault("tf", "2000"));
                String integ = opts.getOrDefault("integrator", "velocity_verlet");
                if (opts.containsKey("dts") || opts.containsKey("ks")) {
                    double[] ks = parseDoubles(opts.getOrDefault("ks", "100,1000,10000"));
                    double[] dts = parseDoubles(opts.getOrDefault("dts",
                            "1e-4,5e-4,1e-3,5e-3,1e-2"));
                    EnergyDtScanExperiment.runSweep(N, ks, dts, tfScan, baseSeed, integ, out);
                } else {
                    double[][] ksDts = {
                            {100.0, 3e-3, 5e-3, 1e-2, 5e-2},
                            {1000.0,  5e-4, 3e-3, 5e-3, 1e-2},
                            {10000.0, 5e-4, 1e-3, 3e-3, 5e-3},
                    };
                    for (double[] row : ksDts) {
                        double kVal = row[0];
                        for (int i = 1; i < row.length; i++) {
                            EnergyDtScanExperiment.runOne(N, kVal, row[i], tfScan,
                                    baseSeed, integ, out);
                        }
                    }
                }
            }
            case "timing" -> {
                int[] Ns = parseInts(opts.getOrDefault("Ns", "100,200,300,400,500,600,700,800,900,1000"));
                TimingExperiment.run(Ns, k, tf, baseSeed, out, append);
            }
            case "jvsn" -> {
                int[] Ns = parseInts(opts.getOrDefault("Ns", "100,200,300,400,500,600,700,800,900,1000"));
                double dt = Geometry.dtForK(k);
                double dt2 = Double.parseDouble(opts.getOrDefault("dt2", "0.05"));
                double radialTMin = Double.parseDouble(
                        opts.getOrDefault("tmin", String.valueOf(tf / 2.0)));
                JvsNExperiment.runSweep(Ns, realizations, k, tf, dt, dt2, baseSeed, out, append, radialTMin);
            }
            case "ksweep" -> {
                double[] ks = parseDoubles(opts.getOrDefault("ks", "100,1000,10000"));
                int[] Ns = parseInts(opts.getOrDefault("Ns", "100,200,300,400,500,600,700,800,900,1000"));
                double dt2 = Double.parseDouble(opts.getOrDefault("dt2", "0.05"));
                KSweepExperiment.run(ks, Ns, realizations, tf, dt2, baseSeed, out, append);
            }
            default -> {
                System.err.println("Unknown experiment: " + experiment);
                System.err.println("Available: energy, energy_dt_scan, timing, jvsn, ksweep");
                System.exit(1);
            }
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String val = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : "true";
                map.put(key, val);
            }
        }
        return map;
    }

    private static int[] parseInts(String csv) {
        String[] parts = csv.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    private static double[] parseDoubles(String csv) {
        String[] parts = csv.split(",");
        double[] arr = new double[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Double.parseDouble(parts[i].trim());
        return arr;
    }
}
