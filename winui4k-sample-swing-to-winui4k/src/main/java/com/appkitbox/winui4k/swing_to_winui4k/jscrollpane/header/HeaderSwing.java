package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.header;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 15.6 Show line numbers that scroll with the content (Swing). */
public class HeaderSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("15.6 Header (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel lines = new JPanel(new GridLayout(0, 1));
            JPanel numbers = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 40; i++) {
                lines.add(new JLabel("Line " + i));
                numbers.add(new JLabel(String.valueOf(i)));
            }
            numbers.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));    // top, left, bottom, right
            JScrollPane scroll = new JScrollPane(lines);
            scroll.setRowHeaderView(numbers);
            scroll.setColumnHeaderView(new JLabel("Text"));

            frame.add(scroll, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            scroll.getVerticalScrollBar().setValue(100);    // scroll once
        });
    }
}
