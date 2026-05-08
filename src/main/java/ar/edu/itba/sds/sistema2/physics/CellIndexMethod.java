package ar.edu.itba.sds.sistema2.physics;

import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;

import java.util.List;

public final class CellIndexMethod {
    private final double cellSize;
    private final int M;
    private final double origin;
    private final int[] head;
    private final int[] next;
    private final int N;

    public CellIndexMethod(int N, double cellSize) {
        this.N = N;
        this.cellSize = cellSize;
        this.M = (int) Math.ceil(Geometry.L / cellSize);
        this.origin = -Geometry.R;
        this.head = new int[M * M];
        this.next = new int[N];
    }

    public void rebuild(List<Particle> particles) {
        for (int i = 0; i < head.length; i++) head[i] = -1;
        for (int i = 0; i < N; i++) {
            Particle p = particles.get(i);
            int cx = clamp((int) Math.floor((p.pos.x - origin) / cellSize));
            int cy = clamp((int) Math.floor((p.pos.y - origin) / cellSize));
            int idx = cy * M + cx;
            next[i] = head[idx];
            head[idx] = i;
        }
    }

    private int clamp(int c) {
        if (c < 0) return 0;
        if (c >= M) return M - 1;
        return c;
    }

    public interface PairConsumer {
        void accept(int i, int j);
    }

    public void forEachPair(List<Particle> particles, PairConsumer consumer) {
        for (int cy = 0; cy < M; cy++) {
            for (int cx = 0; cx < M; cx++) {
                int idx = cy * M + cx;
                for (int i = head[idx]; i != -1; i = next[i]) {
                    for (int j = next[i]; j != -1; j = next[j]) {
                        consumer.accept(i, j);
                    }
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dy == 0 && dx <= 0) continue;
                            int nx = cx + dx;
                            int ny = cy + dy;
                            if (nx < 0 || nx >= M || ny < 0 || ny >= M) continue;
                            int nIdx = ny * M + nx;
                            for (int j = head[nIdx]; j != -1; j = next[j]) {
                                consumer.accept(i, j);
                            }
                        }
                    }
                }
            }
        }
    }
}
