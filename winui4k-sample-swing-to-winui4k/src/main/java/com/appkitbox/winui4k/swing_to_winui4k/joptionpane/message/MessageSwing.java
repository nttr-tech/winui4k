package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.message;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 26.2 Show a message (Swing). */
public class MessageSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("26.2 Message (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JOptionPane.showMessageDialog(frame, "The file could not be saved.",
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
            status.setText("The message was closed");
        });
    }
}
