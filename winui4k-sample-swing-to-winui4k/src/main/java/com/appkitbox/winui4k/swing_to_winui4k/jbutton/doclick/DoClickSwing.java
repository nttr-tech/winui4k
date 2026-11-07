package com.appkitbox.winui4k.swing_to_winui4k.jbutton.doclick;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 1.10 Click programmatically (Swing). */
public class DoClickSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.10 doClick (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel message = new JLabel("Not searched yet");
            JButton search = new JButton("Search");
            search.addActionListener(e -> message.setText("Searched"));

            frame.add(search);
            frame.add(message);

            search.doClick();

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
