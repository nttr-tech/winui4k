package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.sync;

import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 16.4 Scroll two panes together (Swing). */
public class SyncSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("16.4 Sync (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new GridLayout(1, 2));

            JPanel original = new JPanel(new GridLayout(0, 1));
            JPanel translated = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 50; i++) {
                original.add(new JLabel("Original " + i));
                translated.add(new JLabel("Translated " + i));
            }
            JScrollPane left = new JScrollPane(original);
            JScrollPane right = new JScrollPane(translated);
            left.getVerticalScrollBar().addAdjustmentListener(
                    e -> right.getVerticalScrollBar().setValue(e.getValue()));

            frame.add(left);
            frame.add(right);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            left.getVerticalScrollBar().setValue(200);      // scroll the left pane once
        });
    }
}
