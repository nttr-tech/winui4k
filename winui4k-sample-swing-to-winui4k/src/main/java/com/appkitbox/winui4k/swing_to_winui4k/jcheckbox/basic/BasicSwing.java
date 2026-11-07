package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.basic;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 5.2 Read the check state (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("5.2 Read the check state (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JCheckBox remember = new JCheckBox("Remember me", true);
            JCheckBox newsletter = new JCheckBox("Subscribe to the newsletter");
            JLabel result = new JLabel();
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> result.setText(
                    "remember=" + remember.isSelected() + ", newsletter=" + newsletter.isSelected()));

            frame.add(remember);
            frame.add(newsletter);
            frame.add(ok);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            ok.doClick(); // show the result once at startup
        });
    }
}
