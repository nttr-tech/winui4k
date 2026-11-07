package com.appkitbox.winui4k.swing_to_winui4k.jbutton.defaultbutton;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 1.7 Default button pressed by Enter (Swing). */
public class DefaultButtonSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.7 Default Button (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            frame.getRootPane().setDefaultButton(ok);

            frame.add(ok);
            frame.add(cancel);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
