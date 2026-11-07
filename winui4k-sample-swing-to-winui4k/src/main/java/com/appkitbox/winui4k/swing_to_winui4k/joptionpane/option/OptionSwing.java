package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.option;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 26.6 Use your own button labels (Swing). */
public class OptionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("26.6 Option Buttons (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            String[] options = {"Save", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(frame, "Save changes to report.txt?", "Notepad",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                status.setText("Saved");
            }
        });
    }
}
