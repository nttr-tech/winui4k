package com.appkitbox.winui4k.swing_to_winui4k.jdialog.basic;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 25.2 Open an OK / Cancel dialog and read the input (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("25.2 OK and Cancel (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Renaming...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JTextField name = new JTextField("report.txt", 16);
            JDialog dialog = new JDialog(frame, "Rename", true);   // modal
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            boolean[] accepted = {false};
            ok.addActionListener(e -> {
                accepted[0] = true;
                dialog.dispose();
            });
            cancel.addActionListener(e -> dialog.dispose());
            JPanel buttons = new JPanel();
            buttons.add(ok);
            buttons.add(cancel);
            dialog.add(new JLabel("New name:"), BorderLayout.NORTH);
            dialog.add(name, BorderLayout.CENTER);
            dialog.add(buttons, BorderLayout.SOUTH);
            dialog.getRootPane().setDefaultButton(ok);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);              // blocks until the dialog closes
            if (accepted[0]) {
                status.setText("Renamed to " + name.getText());
            }
        });
    }
}
