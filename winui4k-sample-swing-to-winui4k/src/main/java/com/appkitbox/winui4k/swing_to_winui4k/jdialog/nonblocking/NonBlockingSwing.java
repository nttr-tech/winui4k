package com.appkitbox.winui4k.swing_to_winui4k.jdialog.nonblocking;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 25.5 Open a dialog without blocking (Swing). */
public class NonBlockingSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("25.5 Without Blocking (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Opening the Find dialog...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JTextField text = new JTextField("needle", 12);
            JDialog dialog = new JDialog(frame, "Find", false);   // modeless
            dialog.setLayout(new FlowLayout());
            JButton find = new JButton("Find");
            find.addActionListener(e -> status.setText("Searching for " + text.getText()));
            dialog.add(text);
            dialog.add(find);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);                 // returns immediately
            status.setText("The Find dialog is open");
        });
    }
}
