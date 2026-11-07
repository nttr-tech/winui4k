package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.listener;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/** 6.4 React to the selection (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("6.4 React to the selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel shipping = new JLabel("Shipping: Standard");
            ActionListener listener = e -> shipping.setText("Shipping: " + e.getActionCommand());
            JRadioButton standard = new JRadioButton("Standard", true);
            JRadioButton express = new JRadioButton("Express");
            standard.addActionListener(listener);
            express.addActionListener(listener);
            ButtonGroup group = new ButtonGroup();
            group.add(standard);
            group.add(express);

            frame.add(standard);
            frame.add(express);
            frame.add(shipping);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            express.doClick(); // click "Express" once at startup
        });
    }
}
