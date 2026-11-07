package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.basic;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 15.2 Scroll a panel that is larger than the window (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("15.2 Basic (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel options = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 30; i++) {
                options.add(new JCheckBox("Option " + i));
            }
            JScrollPane scroll = new JScrollPane(options);

            frame.add(scroll, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
