package com.appkitbox.winui4k.swing_to_winui4k.jlabel.labelfor;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 4.6 Label for an input field (Swing). */
public class LabelForSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.6 Label for a field (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel nameLabel = new JLabel("Name:");
            JTextField name = new JTextField(15);
            nameLabel.setLabelFor(name);
            nameLabel.setDisplayedMnemonic(KeyEvent.VK_N);         // Alt+N moves the focus to the field

            JLabel emailLabel = new JLabel("Email:");
            JTextField email = new JTextField(15);
            emailLabel.setLabelFor(email);
            emailLabel.setDisplayedMnemonic(KeyEvent.VK_E);

            frame.add(nameLabel);
            frame.add(name);
            frame.add(emailLabel);
            frame.add(email);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
