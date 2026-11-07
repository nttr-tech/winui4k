package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.select;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 26.5 Choose from a list (Swing). */
public class SelectSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("26.5 Select (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            String[] sizes = {"Small", "Medium", "Large"};
            Object size = JOptionPane.showInputDialog(frame, "Choose a size:", "Size",
                    JOptionPane.QUESTION_MESSAGE, null, sizes, "Medium");
            if (size != null) {
                status.setText("Size: " + size);
            }
        });
    }
}
