package com.appkitbox.winui4k.swing_to_winui4k.jdialog.validate;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 25.3 Close the dialog only when the input is valid (Swing). */
public class ValidateSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("25.3 Validate Input (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel(" ");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JTextField age = new JTextField("abc", 8);
            JLabel error = new JLabel(" ");
            error.setForeground(Color.RED);
            JDialog dialog = new JDialog(frame, "Age", true);
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> {
                if (!age.getText().matches("\\d+")) {
                    error.setText("Enter a number");   // keep the dialog open
                    return;
                }
                status.setText("Age: " + age.getText());
                dialog.dispose();
            });
            JPanel content = new JPanel(new GridLayout(0, 1, 4, 4));
            content.add(new JLabel("Your age:"));
            content.add(age);
            content.add(error);
            content.add(ok);
            dialog.add(content);
            dialog.getRootPane().setDefaultButton(ok);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }
}
