package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.embed;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 28.3 Put a color chooser on the window and follow its changes (Swing). */
public class EmbedSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("28.3 Embed (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JColorChooser chooser = new JColorChooser(Color.ORANGE);
            JLabel preview = new JLabel("Sample text");
            preview.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            chooser.getSelectionModel().addChangeListener(e -> preview.setForeground(chooser.getColor()));

            frame.add(chooser);
            frame.add(preview);

            frame.setSize(720, 520);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            chooser.setColor(0, 120, 215);
        });
    }
}
