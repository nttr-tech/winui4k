package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.listener;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/** 15.5 React to scrolling (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("15.5 Listener (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel items = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 50; i++) {
                items.add(new JLabel("Item " + i));
            }
            JScrollPane scroll = new JScrollPane(items);
            JLabel status = new JLabel("Top: 0");

            JViewport viewport = scroll.getViewport();
            viewport.addChangeListener(e -> {
                Point p = viewport.getViewPosition();
                status.setText("Top: " + p.y + " / " + viewport.getViewSize().height);
            });

            frame.add(scroll, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            viewport.setViewPosition(new Point(0, 120));    // scroll once
        });
    }
}
