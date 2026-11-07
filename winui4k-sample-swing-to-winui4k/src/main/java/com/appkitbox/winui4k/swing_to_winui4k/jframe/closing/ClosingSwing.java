package com.appkitbox.winui4k.swing_to_winui4k.jframe.closing;

import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 24.3 Confirm before the window closes (Swing). */
public class ClosingSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("24.3 Confirm Closing (Swing)");
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("Close the window with the X button");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    status.setText("Opened. Close the window with the X button");
                }

                @Override
                public void windowClosing(WindowEvent e) {
                    int answer = JOptionPane.showConfirmDialog(frame, "Discard unsaved changes and exit?",
                            "Exit", JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        frame.dispose();
                    }
                }
            });
            frame.add(status);

            frame.setSize(480, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
