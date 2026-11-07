package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.confirm;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 26.3 Ask yes or no (Swing). */
public class ConfirmSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("26.3 Confirm (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            int answer = JOptionPane.showConfirmDialog(frame, "Delete the selected file?",
                    "Delete", JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                status.setText("Deleted");
            }
        });
    }
}
