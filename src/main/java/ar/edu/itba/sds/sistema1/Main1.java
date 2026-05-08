package ar.edu.itba.sds.sistema1;

import ar.edu.itba.sds.sistema1.analysis.ECMSweep;

import java.io.IOException;
import java.nio.file.Path;

public final class Main1 {
    public static void main(String[] args) throws IOException {
        Path out = Path.of("results", "s1");
        System.out.println("[S1] Computing ECM sweep over dt ∈ {1e-2, ..., 1e-6}");
        ECMSweep.writeSweep(out);
        System.out.println("[S1] Writing reference trajectories at dt = 1e-3");
        ECMSweep.writeReferenceTrajectories(out, 1.0e-3);
        System.out.println("[S1] Done. Output: " + out.toAbsolutePath());
    }
}
