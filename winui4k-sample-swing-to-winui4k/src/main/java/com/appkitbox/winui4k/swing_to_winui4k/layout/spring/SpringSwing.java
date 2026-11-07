package com.appkitbox.winui4k.swing_to_winui4k.layout.spring;

import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/** 30.7 SpringLayout (Swing). */
public class SpringSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.7 SpringLayout (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Container pane = frame.getContentPane();
            SpringLayout layout = new SpringLayout();
            pane.setLayout(layout);

            JLabel label = new JLabel("Name:");
            JTextField field = new JTextField(15);
            JButton ok = new JButton("OK");
            pane.add(label);
            pane.add(field);
            pane.add(ok);

            // the label at (10, 10) from the top left of the pane
            layout.putConstraint(SpringLayout.WEST, label, 10, SpringLayout.WEST, pane);
            layout.putConstraint(SpringLayout.NORTH, label, 10, SpringLayout.NORTH, pane);
            // the field 5 to the right of the label, on the same baseline
            layout.putConstraint(SpringLayout.WEST, field, 5, SpringLayout.EAST, label);
            layout.putConstraint(SpringLayout.BASELINE, field, 0, SpringLayout.BASELINE, label);
            // the button 10 from the bottom right of the pane
            layout.putConstraint(SpringLayout.EAST, ok, -10, SpringLayout.EAST, pane);
            layout.putConstraint(SpringLayout.SOUTH, ok, -10, SpringLayout.SOUTH, pane);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
