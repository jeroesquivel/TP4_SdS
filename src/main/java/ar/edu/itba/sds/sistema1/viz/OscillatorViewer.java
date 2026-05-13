package ar.edu.itba.sds.sistema1.viz;

import ar.edu.itba.sds.sistema1.core.Oscillator;
import ar.edu.itba.sds.sistema1.integrators.BeemanIntegrator;
import ar.edu.itba.sds.sistema1.integrators.EulerIntegrator;
import ar.edu.itba.sds.sistema1.integrators.GearPC5Integrator;
import ar.edu.itba.sds.sistema1.integrators.Integrator;
import ar.edu.itba.sds.sistema1.integrators.VerletIntegrator;

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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public final class OscillatorViewer extends JFrame {
    private static final double X_MIN = -Oscillator.A * 1.4;
    private static final double X_MAX = Oscillator.A * 1.4;

    private final SimPanel canvas = new SimPanel();
    private final Timer timer;

    private final JComboBox<String> integCombo =
            new JComboBox<>(new String[]{"euler", "verlet", "beeman", "gear5"});
    private final JComboBox<String> dtCombo =
            new JComboBox<>(new String[]{"1e-2", "1e-3", "1e-4"});
    private final JSpinner stepsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 200, 1));
    private final JButton playBtn = new JButton("Play");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton resetBtn = new JButton("Reset");
    private final JLabel statusLbl = new JLabel(" ");

    private Integrator integrator;
    private double dt;
    private double t;
    private boolean running;
    private final List<double[]> trace = new ArrayList<>();   // {t, r_num, r_ana}

    public OscillatorViewer() {
        super("TP4 — Sistema 1 (oscilador amortiguado)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        dtCombo.setSelectedItem("1e-3");

        JPanel paramsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        paramsRow.add(new JLabel("integrador:"));
        paramsRow.add(integCombo);
        paramsRow.add(new JLabel("Δt [s]:"));
        paramsRow.add(dtCombo);
        paramsRow.add(new JLabel("pasos/frame:"));
        paramsRow.add(stepsSpinner);

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
        setSize(900, 720);
        setLocationRelativeTo(null);
    }

    private void reset() {
        running = false;
        playBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        timer.stop();

        dt = Double.parseDouble((String) dtCombo.getSelectedItem());
        integrator = newIntegrator((String) integCombo.getSelectedItem(), dt);
        t = 0.0;
        trace.clear();
        trace.add(new double[]{0.0, integrator.r(), Oscillator.analyticalR(0.0)});

        updateStatus();
        canvas.repaint();
    }

    private static Integrator newIntegrator(String name, double dt) {
        return switch (name) {
            case "verlet" -> new VerletIntegrator(Oscillator.R0, Oscillator.V0, dt);
            case "beeman" -> new BeemanIntegrator(Oscillator.R0, Oscillator.V0, dt);
            case "gear5"  -> new GearPC5Integrator(Oscillator.R0, Oscillator.V0, dt);
            default       -> new EulerIntegrator(Oscillator.R0, Oscillator.V0, dt);
        };
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
            integrator.step();
            t += dt;
            trace.add(new double[]{t, integrator.r(), Oscillator.analyticalR(t)});
        }
        if (t >= Oscillator.T_F) pause();
        updateStatus();
        canvas.repaint();
    }

    private void updateStatus() {
        double rNum = integrator.r();
        double rAna = Oscillator.analyticalR(t);
        statusLbl.setText(String.format(
                "  integrador = %s  |  Δt = %.0e s  |  t = %.4f s  |  r_num = %+.5f  |  r_ana = %+.5f  |  |error| = %.2e",
                integrator.name(), dt, t, rNum, rAna, Math.abs(rNum - rAna)));
    }

    private final class SimPanel extends JPanel {
        SimPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(880, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int hTop = h / 3;                         // animación física
            int hBot = h - hTop - 10;                 // r(t)

            paintTop(g2, w, hTop);
            paintBottom(g2, 0, hTop + 10, w, hBot);

            g2.dispose();
        }

        private void paintTop(Graphics2D g2, int w, int h) {
            int margin = 60;
            int axisY = h / 2;
            double scale = (w - 2 * margin) / (X_MAX - X_MIN);
            int x0 = (int) (margin - X_MIN * scale);  // pixel para x=0
            int xMinPx = (int) (margin);
            int xMaxPx = (int) (w - margin);

            // Eje horizontal
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(xMinPx, axisY, xMaxPx, axisY);

            // Marca de equilibrio
            g2.setColor(new Color(150, 150, 150));
            g2.drawLine(x0, axisY - 6, x0, axisY + 6);
            g2.drawString("0", x0 - 4, axisY + 22);

            // Pared izquierda (anclaje del resorte)
            int xWall = xMinPx - 10;
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(xWall, axisY - 30, xWall, axisY + 30);

            // Posiciones actual (numérica) y analítica
            double rNum = integrator.r();
            double rAna = Oscillator.analyticalR(t);
            int xNum = (int) (x0 + rNum * scale);
            int xAna = (int) (x0 + rAna * scale);

            // Resorte (zigzag de la pared al mass numérico)
            drawSpring(g2, xWall, axisY, xNum, axisY);

            // Sombra de la posición analítica
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(xAna - 14, axisY - 14, 28, 28);

            // Mass numérico
            g2.setColor(new Color(40, 110, 220));
            g2.fillOval(xNum - 16, axisY - 16, 32, 32);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(xNum - 16, axisY - 16, 32, 32);

            // Etiquetas
            g2.setColor(new Color(80, 80, 80));
            g2.drawString("oscilador", xWall + 8, 18);
            g2.drawString("● numérico", xMaxPx - 90, 18);
            g2.drawString("● analítico", xMaxPx - 90, 32);
        }

        private void drawSpring(Graphics2D g2, int x1, int y, int x2, int y2) {
            int n = 20;
            int amp = 8;
            g2.setColor(new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(1.6f));
            int xPrev = x1;
            int yPrev = y;
            for (int i = 1; i <= n; i++) {
                double f = i / (double) n;
                int xi = (int) (x1 + f * (x2 - x1));
                int yi = y + ((i % 2 == 0) ? -amp : amp);
                if (i == n) yi = y2;
                g2.draw(new Line2D.Float(xPrev, yPrev, xi, yi));
                xPrev = xi;
                yPrev = yi;
            }
        }

        private void paintBottom(Graphics2D g2, int x0, int y0, int w, int h) {
            int marginL = 50;
            int marginR = 20;
            int marginT = 20;
            int marginB = 30;

            int plotX0 = x0 + marginL;
            int plotY0 = y0 + marginT;
            int plotW = w - marginL - marginR;
            int plotH = h - marginT - marginB;

            // Caja
            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(plotX0, plotY0, plotW, plotH);
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(plotX0, plotY0, plotW, plotH);

            double tMax = Oscillator.T_F;
            double yMax = Oscillator.A * 1.05;
            double yMin = -yMax;

            // Eje cero
            int yZero = (int) (plotY0 + plotH * (yMax / (yMax - yMin)));
            g2.setColor(new Color(180, 180, 180));
            g2.drawLine(plotX0, yZero, plotX0 + plotW, yZero);

            // Etiquetas
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("r [m]", plotX0 - 40, plotY0 + 12);
            g2.drawString("t [s]", plotX0 + plotW - 30, plotY0 + plotH + 22);
            g2.drawString(String.format("%.1f", yMax),  plotX0 - 40, plotY0 + 12);
            g2.drawString("0",                          plotX0 - 12, yZero + 4);
            g2.drawString(String.format("%.1f", yMin),  plotX0 - 40, plotY0 + plotH);
            g2.drawString("0", plotX0 - 4, plotY0 + plotH + 14);
            g2.drawString(String.format("%.1f", tMax), plotX0 + plotW - 14, plotY0 + plotH + 14);

            if (trace.size() < 2) return;

            // Trazo analítico (denso, no depende de la simulación)
            g2.setColor(new Color(0, 0, 0, 180));
            g2.setStroke(new BasicStroke(1.6f));
            int prevX = -1;
            int prevY = -1;
            int N = 600;
            for (int i = 0; i <= N; i++) {
                double tt = tMax * i / N;
                double rr = Oscillator.analyticalR(tt);
                int px = (int) (plotX0 + plotW * (tt / tMax));
                int py = (int) (plotY0 + plotH * ((yMax - rr) / (yMax - yMin)));
                if (prevX != -1) g2.drawLine(prevX, prevY, px, py);
                prevX = px;
                prevY = py;
            }

            // Trazo numérico (de la simulación)
            g2.setColor(new Color(40, 110, 220));
            g2.setStroke(new BasicStroke(1.8f));
            prevX = -1;
            prevY = -1;
            int stride = Math.max(1, trace.size() / 2000);
            for (int i = 0; i < trace.size(); i += stride) {
                double[] p = trace.get(i);
                int px = (int) (plotX0 + plotW * (p[0] / tMax));
                int py = (int) (plotY0 + plotH * ((yMax - p[1]) / (yMax - yMin)));
                if (prevX != -1) g2.drawLine(prevX, prevY, px, py);
                prevX = px;
                prevY = py;
            }

            // Línea vertical de t actual
            int xNow = (int) (plotX0 + plotW * Math.min(t / tMax, 1.0));
            g2.setColor(new Color(220, 100, 30, 180));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(xNow, plotY0, xNow, plotY0 + plotH);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OscillatorViewer().setVisible(true));
    }
}
