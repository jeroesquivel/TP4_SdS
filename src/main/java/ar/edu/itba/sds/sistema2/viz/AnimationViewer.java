package ar.edu.itba.sds.sistema2.viz;

import ar.edu.itba.sds.sistema2.physics.CellIndexMethod;
import ar.edu.itba.sds.sistema2.core.ConfigSeeder;
import ar.edu.itba.sds.sistema2.physics.ForceModel;
import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.sim.Simulator2D;
import ar.edu.itba.sds.sistema2.integrators.BeemanIntegrator2D;
import ar.edu.itba.sds.sistema2.integrators.Integrator2D;
import ar.edu.itba.sds.sistema2.integrators.VelocityVerletIntegrator2D;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.List;

public final class AnimationViewer extends JFrame {

    private final SimPanel canvas = new SimPanel();
    private final Timer timer;

    private final JSpinner nSpinner = new JSpinner(new SpinnerNumberModel(200, 100, 1000, 50));
    private final JComboBox<String> kCombo = new JComboBox<>(new String[]{"1e2", "1e3", "1e4", "1e5"});
    private final JComboBox<String> integCombo = new JComboBox<>(new String[]{"velocity_verlet", "beeman"});
    private final JSpinner stepsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 200, 1));
    private final JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(42L, 0L, Long.MAX_VALUE, 1L));
    private final JButton playBtn = new JButton("Play");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton resetBtn = new JButton("Reset");
    private final JLabel statusLbl = new JLabel(" ");

    private List<Particle> particles;
    private ForceModel forceModel;
    private Integrator2D integrator;
    private double dt;
    private double t;
    private boolean running;

    public AnimationViewer() {
        super("TP4 — Sistema 2 (recinto circular con obstáculo)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        kCombo.setSelectedItem("1e3");

        JPanel paramsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        paramsRow.add(new JLabel("N:"));
        paramsRow.add(nSpinner);
        paramsRow.add(new JLabel("k:"));
        paramsRow.add(kCombo);
        paramsRow.add(new JLabel("integrador:"));
        paramsRow.add(integCombo);
        paramsRow.add(new JLabel("pasos/frame:"));
        paramsRow.add(stepsSpinner);
        paramsRow.add(new JLabel("seed:"));
        paramsRow.add(seedSpinner);

        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controlsRow.add(playBtn);
        controlsRow.add(pauseBtn);
        controlsRow.add(resetBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(paramsRow);
        north.add(controlsRow);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLbl, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        playBtn.addActionListener(e -> play());
        pauseBtn.addActionListener(e -> pause());
        resetBtn.addActionListener(e -> reset());
        pauseBtn.setEnabled(false);

        timer = new Timer(16, e -> tick());
        timer.setCoalesce(true);

        reset();
        setSize(820, 880);
        setLocationRelativeTo(null);
    }

    private double currentK() {
        return Double.parseDouble((String) kCombo.getSelectedItem());
    }

    private void reset() {
        running = false;
        playBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        timer.stop();

        int N = ((Number) nSpinner.getValue()).intValue();
        long seed = ((Number) seedSpinner.getValue()).longValue();
        double k = currentK();

        particles = ConfigSeeder.seed(N, seed);
        double cell = Math.max(2.0 * Geometry.R_PARTICLE, Geometry.L / 40.0);
        CellIndexMethod cim = new CellIndexMethod(N, cell);
        forceModel = new ForceModel(k, cim);
        integrator = "beeman".equals(integCombo.getSelectedItem())
                ? new BeemanIntegrator2D(particles, forceModel)
                : new VelocityVerletIntegrator2D(particles, forceModel);
        dt = Geometry.dtForK(k);
        t = 0.0;

        canvas.setParticles(particles);
        updateStatus();
        canvas.repaint();
    }

    private void play() {
        if (running) return;
        running = true;
        playBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        timer.start();
    }

    private void pause() {
        if (!running) return;
        running = false;
        playBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        timer.stop();
    }

    private void tick() {
        int steps = (Integer) stepsSpinner.getValue();
        for (int i = 0; i < steps; i++) {
            integrator.step(dt);
            t += dt;
            updateStates();
        }
        updateStatus();
        canvas.repaint();
    }

    private void updateStates() {
        for (Particle p : particles) {
            double r = p.pos.norm();
            boolean nowObs = r <= Geometry.R_OBSTACLE + p.radius;
            boolean nowBor = r >= Geometry.R - p.radius;
            if (p.state == Particle.FRESH && nowObs) p.state = Particle.USED;
            else if (p.state == Particle.USED && nowBor) p.state = Particle.FRESH;
            p.inContactObs = nowObs;
            p.inContactBorder = nowBor;
        }
    }

    private void updateStatus() {
        double kE = 0.0;
        int fresh = 0;
        for (Particle p : particles) {
            kE += 0.5 * p.mass * (p.vel.x * p.vel.x + p.vel.y * p.vel.y);
            if (p.state == Particle.FRESH) fresh++;
        }
        double pE = forceModel.potentialEnergy();
        statusLbl.setText(String.format(
                "  t = %.3f s  |  N = %d  |  k = %.0e  |  dt = %.2e  |  Ek = %.2f  |  Ep = %.2f  |  E = %.2f  |  fresh = %d / %d",
                t, particles.size(), currentK(), dt, kE, pE, kE + pE, fresh, particles.size()));
    }

    private static final class SimPanel extends JPanel {
        private List<Particle> particles;

        SimPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(800, 800));
        }

        void setParticles(List<Particle> ps) { this.particles = ps; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            double size = Math.min(w, h) - 20;
            double cx = w / 2.0;
            double cy = h / 2.0;
            double scale = size / Geometry.L;

            g2.setColor(new Color(245, 245, 245));
            g2.fillOval((int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size);
            g2.setColor(Color.BLACK);
            g2.drawOval((int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size);

            double obsR = Geometry.R_OBSTACLE * scale;
            g2.setColor(new Color(80, 80, 80));
            g2.fillOval((int) (cx - obsR), (int) (cy - obsR), (int) (2 * obsR), (int) (2 * obsR));

            if (particles != null) {
                Color fresh = new Color(40, 110, 220);
                Color used = new Color(230, 130, 30);
                Ellipse2D.Double e = new Ellipse2D.Double();
                for (Particle p : particles) {
                    double sx = cx + p.pos.x * scale;
                    double sy = cy - p.pos.y * scale;
                    double sr = p.radius * scale;
                    g2.setColor(p.state == Particle.FRESH ? fresh : used);
                    e.setFrame(sx - sr, sy - sr, 2 * sr, 2 * sr);
                    g2.fill(e);
                }
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AnimationViewer().setVisible(true));
    }
}
