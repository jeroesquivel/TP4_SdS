package ar.edu.itba.sds.sistema2.viz;

import ar.edu.itba.sds.sistema2.core.ConfigSeeder;
import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.integrators.BeemanIntegrator2D;
import ar.edu.itba.sds.sistema2.integrators.Integrator2D;
import ar.edu.itba.sds.sistema2.integrators.VelocityVerletIntegrator2D;
import ar.edu.itba.sds.sistema2.physics.CellIndexMethod;
import ar.edu.itba.sds.sistema2.physics.ForceModel;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Renderizador headless: corre la simulación y escribe una secuencia
 * frame_0000.png ... frame_NNNN.png en el directorio destino, lista para
 * el paquete `animate` de LaTeX (\animategraphics).
 *
 * CLI (vía Main2 --experiment animate): N, k, tf, fps, width, out, integrator, seed.
 */
public final class FrameExporter {

    public static void render(int N, double k, double tf, int fps, int width,
                              String integrator, long seed, File outDir) throws IOException {
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio " + outDir);
        }

        List<Particle> particles = ConfigSeeder.seed(N, seed);
        double cell = Math.max(2.0 * Geometry.R_PARTICLE, Geometry.L / 40.0);
        CellIndexMethod cim = new CellIndexMethod(N, cell);
        ForceModel forceModel = new ForceModel(k, cim);
        Integrator2D integ = "beeman".equals(integrator)
                ? new BeemanIntegrator2D(particles, forceModel)
                : new VelocityVerletIntegrator2D(particles, forceModel);

        double dt = Geometry.dtForK(k);
        double frameDt = 1.0 / fps;
        int stepsPerFrame = Math.max(1, (int) Math.round(frameDt / dt));
        int totalFrames = (int) Math.round(tf * fps);

        int height = width;
        double t = 0.0;

        for (int frame = 0; frame < totalFrames; frame++) {
            writeFrame(particles, width, height, new File(outDir, frameName(frame)));
            for (int s = 0; s < stepsPerFrame; s++) {
                integ.step(dt);
                t += dt;
                updateStates(particles);
            }
            if (frame % 25 == 0) {
                System.out.printf("  frame %d/%d  t=%.3fs%n", frame, totalFrames, t);
            }
        }
        System.out.printf("Listo: %d frames escritos en %s%n", totalFrames, outDir);
    }

    private static String frameName(int i) {
        // El paquete `animate` de LaTeX espera `frame_0.png, frame_1.png, ...`
        // sin padding de ceros.
        return "frame_" + i + ".png";
    }

    private static void updateStates(List<Particle> ps) {
        for (Particle p : ps) {
            double r = p.pos.norm();
            boolean nowObs = r <= Geometry.R_OBSTACLE + p.radius;
            boolean nowBor = r >= Geometry.R - p.radius;
            if (p.state == Particle.FRESH && nowObs) p.state = Particle.USED;
            else if (p.state == Particle.USED && nowBor) p.state = Particle.FRESH;
            p.inContactObs = nowObs;
            p.inContactBorder = nowBor;
        }
    }

    private static void writeFrame(List<Particle> particles, int w, int h, File out) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        double size = Math.min(w, h) - 20;
        double cx = w / 2.0;
        double cy = h / 2.0;
        double scale = size / Geometry.L;

        g.setColor(new Color(245, 245, 245));
        g.fillOval((int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size);
        g.setColor(Color.BLACK);
        g.drawOval((int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size);

        double obsR = Geometry.R_OBSTACLE * scale;
        g.setColor(new Color(80, 80, 80));
        g.fillOval((int) (cx - obsR), (int) (cy - obsR), (int) (2 * obsR), (int) (2 * obsR));

        Color fresh = new Color(40, 110, 220);
        Color used = new Color(230, 130, 30);
        Ellipse2D.Double e = new Ellipse2D.Double();
        for (Particle p : particles) {
            double sx = cx + p.pos.x * scale;
            double sy = cy - p.pos.y * scale;
            double sr = p.radius * scale;
            g.setColor(p.state == Particle.FRESH ? fresh : used);
            e.setFrame(sx - sr, sy - sr, 2 * sr, 2 * sr);
            g.fill(e);
        }

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private FrameExporter() {}
}
