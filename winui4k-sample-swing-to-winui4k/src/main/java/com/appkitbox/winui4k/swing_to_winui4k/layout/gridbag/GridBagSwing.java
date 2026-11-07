package com.appkitbox.winui4k.swing_to_winui4k.layout.gridbag;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 30.6 GridBagLayout (Swing). */
public class GridBagSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.6 GridBagLayout (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);

            // column 0: labels at their own width
            c.gridx = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.gridy = 0;
            frame.add(new JLabel("Name:"), c);
            c.gridy = 1;
            frame.add(new JLabel("Email:"), c);

            // column 1: fields take the rest of the width
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            c.gridy = 0;
            frame.add(new JTextField(), c);
            c.gridy = 1;
            frame.add(new JTextField(), c);

            // row 2: a button spanning both columns, at the top right of the remaining height
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.FIRST_LINE_END;
            c.weighty = 1.0;
            frame.add(new JButton("Submit"), c);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
