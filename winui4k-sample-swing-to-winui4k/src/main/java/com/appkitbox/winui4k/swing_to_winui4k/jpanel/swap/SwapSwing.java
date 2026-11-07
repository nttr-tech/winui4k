package com.appkitbox.winui4k.swing_to_winui4k.jpanel.swap;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 14.6 Replace what a panel shows (Swing). */
public class SwapSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("14.6 Swap (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel page = new JPanel(new FlowLayout(FlowLayout.LEFT));
            page.add(new JLabel("Step 1: choose a file"));
            JButton next = new JButton("Next");
            next.addActionListener(e -> {
                page.removeAll();
                page.add(new JLabel("Step 2: confirm"));
                page.add(new JButton("Finish"));
                page.revalidate();
                page.repaint();
            });

            frame.add(page, BorderLayout.CENTER);
            frame.add(next, BorderLayout.SOUTH);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            next.doClick();     // show step 2
        });
    }
}
