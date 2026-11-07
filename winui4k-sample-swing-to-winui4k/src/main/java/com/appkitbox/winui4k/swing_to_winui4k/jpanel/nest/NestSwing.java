package com.appkitbox.winui4k.swing_to_winui4k.jpanel.nest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 14.2 Nest panels (Swing). */
public class NestSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("14.2 Nest (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.add(new JLabel("Name:"));
            form.add(new JTextField(15));

            JPanel options = new JPanel(new GridLayout(0, 1));
            options.add(new JCheckBox("Send me the newsletter"));
            options.add(new JCheckBox("Remember me"));

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(new JButton("OK"));
            buttons.add(new JButton("Cancel"));

            frame.add(form, BorderLayout.NORTH);
            frame.add(options, BorderLayout.CENTER);
            frame.add(buttons, BorderLayout.SOUTH);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
