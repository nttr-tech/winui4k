package com.appkitbox.winui4k.swing_to_winui4k.jlabel.wrap;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 4.7 Wrap long text (Swing). */
public class WrapSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.7 Wrap long text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String message = "Your changes have not been saved. "
                    + "If you close the window now, the changes will be lost.";
            // Swing wraps a label only when the text is HTML with a width
            JLabel wrapped = new JLabel("<html><body style='width: 250px'>" + message + "</body></html>");

            frame.add(wrapped);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
