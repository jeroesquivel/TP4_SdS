package ar.edu.itba.sds.sistema2;

import ar.edu.itba.sds.sistema2.experiments.EnergyExperiment;
import ar.edu.itba.sds.sistema2.experiments.JvsNExperiment;
import ar.edu.itba.sds.sistema2.experiments.KSweepExperiment;
import ar.edu.itba.sds.sistema2.experiments.TimingExperiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Main2 {
    public static void main(String[] args) throws IOException {
        Map<String, String> opts = parseArgs(args);
        String experiment = opts.getOrDefault("experiment", "energy");
        long baseSeed = Long.parseLong(opts.getOrDefault("seed", "42"));
        double tf = Double.parseDouble(opts.getOrDefault("tf", "500"));
        double k = Double.parseDouble(opts.getOrDefault("k", "1000"));
        int realizations = Integer.parseInt(opts.getOrDefault("realizations", "10"));
        Path out = Path.of("results", "s2");

        switch (experiment) {
            case "energy" -> {
                int N = Integer.parseInt(opts.getOrDefault("N", "100"));
                double tfShort = Double.parseDouble(opts.getOrDefault("tf", "5"));
                String integ = opts.getOrDefault("integrator", "velocity_verlet");
                EnergyExperiment.run(N, k, tfShort, baseSeed, integ, out);
            }
            case "timing" -> {
                int[] Ns = parseInts(opts.getOrDefault("Ns", "100,200,400,800,1000"));
                TimingExperiment.run(Ns, k, tf, baseSeed, out);
            }
            case "jvsn" -> {
                int[] Ns = parseInts(opts.getOrDefault("Ns", "10,20,50,100,200,400,800,1000"));
                double dt = Geometry.dtForK(k);
                double dt2 = Double.parseDouble(opts.getOrDefault("dt2", "0.05"));
                JvsNExperiment.runSweep(Ns, realizations, k, tf, dt, dt2, baseSeed, out);
            }
            case "ksweep" -> {
                double[] ks = parseDoubles(opts.getOrDefault("ks", "100,1000,10000"));
                int[] Ns = parseInts(opts.getOrDefault("Ns", "10,20,50,100,200,400,800,1000"));
                double dt2 = Double.parseDouble(opts.getOrDefault("dt2", "0.05"));
                KSweepExperiment.run(ks, Ns, realizations, tf, dt2, baseSeed, out);
            }
            default -> {
                System.err.println("Unknown experiment: " + experiment);
                System.err.println("Available: energy, timing, jvsn, ksweep");
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
