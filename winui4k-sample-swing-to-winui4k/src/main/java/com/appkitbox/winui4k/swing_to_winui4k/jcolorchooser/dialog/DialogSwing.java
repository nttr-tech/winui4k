package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 28.2 Choose a color in a dialog (Swing). */
public class DialogSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("28.2 Dialog (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(80, 40));
            swatch.setBackground(new Color(0, 120, 215));
            JButton choose = new JButton("Choose Color...");
            choose.addActionListener(e -> {
                Color color = JColorChooser.showDialog(frame, "Choose a color", swatch.getBackground());
                if (color != null) {   // null when canceled
                    swatch.setBackground(color);
                }
            });

            frame.add(choose);
            frame.add(swatch);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            choose.doClick();
        });
    }
}
