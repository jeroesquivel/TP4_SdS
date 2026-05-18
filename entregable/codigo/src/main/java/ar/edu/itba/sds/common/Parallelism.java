package ar.edu.itba.sds.common;

import java.util.concurrent.ForkJoinPool;

/**
 * Configura el ForkJoinPool global para usar ~90% de los núcleos disponibles.
 * Se llama una vez al inicio de cada Main (Main1 / Main2). Override por
 * variable de entorno {@code TP4_PARALLELISM} si se quiere otra cantidad.
 */
public final class Parallelism {
    private Parallelism() {}

    public static int configure() {
        int cores = Runtime.getRuntime().availableProcessors();
        int parallel;
        String env = System.getenv("TP4_PARALLELISM");
        if (env != null && !env.isBlank()) {
            parallel = Math.max(1, Integer.parseInt(env.trim()));
        } else {
            // 90% de los núcleos, piso 1.
            parallel = Math.max(1, (int) Math.floor(cores * 0.9));
        }
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                String.valueOf(parallel));
        // Tocar el pool fuerza la inicialización con el valor que acabamos de setear.
        int actual = ForkJoinPool.commonPool().getParallelism();
        System.out.printf("[parallelism] cores=%d  parallel=%d  (commonPool=%d)%n",
                cores, parallel, actual);
        return actual;
    }
}
