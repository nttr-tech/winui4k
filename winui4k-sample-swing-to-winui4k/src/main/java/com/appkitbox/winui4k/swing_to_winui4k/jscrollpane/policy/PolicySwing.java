package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.policy;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/** 15.3 Choose when the scroll bars appear (Swing). */
public class PolicySwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("15.3 Policy (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel lines = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 5; i++) {
                lines.add(new JLabel("Line " + i + ": a long line of text that is much wider than the window, so it needs a horizontal scroll bar"));
            }
            JScrollPane scroll = new JScrollPane(lines,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            frame.add(scroll, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
