package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.listener;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/** 17.4 React when the divider moves (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("17.4 Listener (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JLabel status = new JLabel(" ");
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JLabel("Left"), new JLabel("Right"));
            split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                    e -> status.setText("Divider at " + split.getDividerLocation()
                            + " (was " + split.getLastDividerLocation() + ")"));

            frame.add(split, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            split.setDividerLocation(200);      // move the divider once
        });
    }
}
