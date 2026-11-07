package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.listener;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 16.3 Read the scroll position as a scroll bar value (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("16.3 Listener (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel items = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 50; i++) {
                items.add(new JLabel("Item " + i));
            }
            JScrollPane scroll = new JScrollPane(items);
            JLabel status = new JLabel(" ");

            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.addAdjustmentListener(e -> {
                int percent = 100 * e.getValue() / (bar.getMaximum() - bar.getVisibleAmount());
                status.setText("Value " + e.getValue() + " of " + bar.getMaximum()
                        + " (visible " + bar.getVisibleAmount() + ", " + percent + "%)");
            });

            frame.add(scroll, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            bar.setValue(300);      // scroll once
        });
    }
}
