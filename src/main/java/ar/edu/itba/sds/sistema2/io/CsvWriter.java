package ar.edu.itba.sds.sistema2.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvWriter {
    private CsvWriter() {}

    public static BufferedWriter open(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.newBufferedWriter(path);
    }

    public static void writeLine(BufferedWriter w, String line) throws IOException {
        w.write(line);
        w.newLine();
    }
}
