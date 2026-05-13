package ar.edu.itba.sds.viz;

import ar.edu.itba.sds.sistema1.viz.OscillatorViewer;
import ar.edu.itba.sds.sistema2.viz.AnimationViewer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class AnimationMenu extends JFrame {
    public AnimationMenu() {
        super("TP4 — Animaciones");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Elegí una animación");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(20));

        JButton btnS1 = new JButton("Sistema 1 — Oscilador amortiguado");
        JButton btnS2 = new JButton("Sistema 2 — Recinto circular con obstáculo");
        for (JButton b : new JButton[]{btnS1, btnS2}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(420, 50));
            b.setPreferredSize(new Dimension(420, 50));
            content.add(b);
            content.add(Box.createVerticalStrut(10));
        }

        btnS1.addActionListener(e -> { dispose(); new OscillatorViewer().setVisible(true); });
        btnS2.addActionListener(e -> { dispose(); new AnimationViewer().setVisible(true); });

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AnimationMenu().setVisible(true));
    }
}
