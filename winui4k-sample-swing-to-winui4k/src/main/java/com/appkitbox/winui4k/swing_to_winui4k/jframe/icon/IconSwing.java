package com.appkitbox.winui4k.swing_to_winui4k.jframe.icon;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 24.4 Set the window icon (Swing). */
public class IconSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("24.4 Icon (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            frame.setIconImage(new ImageIcon("icons/app.png").getImage());
            frame.add(new JLabel("The icon appears in the title bar and the taskbar"));

            frame.setSize(420, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
