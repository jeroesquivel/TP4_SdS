package ar.edu.itba.sds.common;

public final class Stopwatch {
    private long startNs;
    private long elapsedNs;
    private boolean running;

    public Stopwatch start() {
        startNs = System.nanoTime();
        running = true;
        return this;
    }

    public Stopwatch stop() {
        if (running) {
            elapsedNs += System.nanoTime() - startNs;
            running = false;
        }
        return this;
    }

    public double elapsedSeconds() {
        long total = elapsedNs + (running ? System.nanoTime() - startNs : 0);
        return total / 1e9;
    }
}
