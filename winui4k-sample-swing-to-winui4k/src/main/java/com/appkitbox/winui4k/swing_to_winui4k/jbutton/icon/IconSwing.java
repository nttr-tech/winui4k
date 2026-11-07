package com.appkitbox.winui4k.swing_to_winui4k.jbutton.icon;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** 1.4 Add an icon (Swing). */
public class IconSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.4 Icon (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton save = new JButton("Save", new ImageIcon("icons/save.png"));
            JButton delete = new JButton(new ImageIcon("icons/delete.png"));  // icon only
            JButton top = new JButton("Download", new ImageIcon("icons/download.png"));
            top.setHorizontalTextPosition(SwingConstants.CENTER);
            top.setVerticalTextPosition(SwingConstants.BOTTOM);              // text below the icon

            frame.add(save);
            frame.add(delete);
            frame.add(top);

            frame.setSize(400, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
