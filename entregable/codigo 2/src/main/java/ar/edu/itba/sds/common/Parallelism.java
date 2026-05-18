package ar.edu.itba.sds.common;

import java.util.concurrent.ForkJoinPool;

/**
 * Crea un ForkJoinPool dedicado con ~90% de los cores. Override via
 * variable de entorno {@code TP4_PARALLELISM}.
 *
 * No usamos {@link ForkJoinPool#commonPool()} porque su tamaño se fija al
 * primer acceso y la system property es sensible al orden de inicialización;
 * en runs reales nos quedó en parallelism=1 sin darnos cuenta.
 */
public final class Parallelism {
    private Parallelism() {}

    private static volatile ForkJoinPool POOL;

    public static int configure() {
        int cores = Runtime.getRuntime().availableProcessors();
        int parallel;
        String env = System.getenv("TP4_PARALLELISM");
        if (env != null && !env.isBlank()) {
            parallel = Math.max(1, Integer.parseInt(env.trim()));
        } else {
            parallel = Math.max(1, (int) Math.floor(cores * 0.9));
        }
        POOL = new ForkJoinPool(parallel);
        System.out.printf("[parallelism] cores=%d  pool=%d%n", cores, parallel);
        return parallel;
    }

    public static ForkJoinPool pool() {
        if (POOL == null) configure();
        return POOL;
    }
}
