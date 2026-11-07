package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.alpha;

import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/** 28.4 Let the user choose the transparency (Swing). */
public class AlphaSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("28.4 Alpha (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JColorChooser chooser = new JColorChooser(new Color(0, 120, 215, 128));
            // keep only the RGB panel, which has an Alpha slider
            // (0: Swatches, 1: HSV, 2: HSL, 3: RGB, 4: CMYK)
            AbstractColorChooserPanel rgb = chooser.getChooserPanels()[3];
            chooser.setChooserPanels(new AbstractColorChooserPanel[] {rgb});
            JLabel alpha = new JLabel("Alpha: " + chooser.getColor().getAlpha());
            chooser.getSelectionModel().addChangeListener(
                e -> alpha.setText("Alpha: " + chooser.getColor().getAlpha()));

            frame.add(chooser);
            frame.add(alpha);

            frame.setSize(720, 520);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
