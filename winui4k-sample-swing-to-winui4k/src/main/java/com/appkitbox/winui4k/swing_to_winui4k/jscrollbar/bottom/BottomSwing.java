package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.bottom;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 16.2 Scroll to the end after adding a line (Swing). */
public class BottomSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("16.2 Bottom (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel log = new JPanel(new GridLayout(0, 1));
            JScrollPane scroll = new JScrollPane(log);
            JButton add = new JButton("Add a line");
            add.addActionListener(e -> {
                log.add(new JLabel("Line " + (log.getComponentCount() + 1)));
                log.revalidate();
                // the maximum grows after the layout, so scroll later
                SwingUtilities.invokeLater(() -> {
                    JScrollBar bar = scroll.getVerticalScrollBar();
                    bar.setValue(bar.getMaximum());
                });
            });

            frame.add(scroll, BorderLayout.CENTER);
            frame.add(add, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            for (int i = 0; i < 30; i++) {
                add.doClick();      // add 30 lines
            }
        });
    }
}
