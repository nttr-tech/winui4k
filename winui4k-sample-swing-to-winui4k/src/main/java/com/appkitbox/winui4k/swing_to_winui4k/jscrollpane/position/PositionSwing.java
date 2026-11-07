package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.position;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 15.4 Scroll from the program (Swing). */
public class PositionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("15.4 Position (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel items = new JPanel(new GridLayout(0, 1));
            for (int i = 1; i <= 50; i++) {
                items.add(new JLabel("Item " + i));
            }
            JScrollPane scroll = new JScrollPane(items);

            JButton top = new JButton("Top");
            top.addActionListener(e -> scroll.getViewport().setViewPosition(new Point(0, 0)));
            JButton down = new JButton("Down 200");
            down.addActionListener(e -> {
                Point p = scroll.getViewport().getViewPosition();
                scroll.getViewport().setViewPosition(new Point(p.x, p.y + 200));
            });
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(top);
            buttons.add(down);

            frame.add(buttons, BorderLayout.NORTH);
            frame.add(scroll, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            down.doClick();     // scroll down once
        });
    }
}
