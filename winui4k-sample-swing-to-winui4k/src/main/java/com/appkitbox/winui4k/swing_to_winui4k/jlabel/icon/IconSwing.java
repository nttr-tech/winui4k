package com.appkitbox.winui4k.swing_to_winui4k.jlabel.icon;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** 4.4 Icon (Swing). */
public class IconSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4.4 Icon (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            ImageIcon person = new ImageIcon("icons/person.png");
            JLabel user = new JLabel("Signed in as alice", person, SwingConstants.LEADING);
            JLabel avatar = new JLabel(person);                   // icon only
            JLabel profile = new JLabel("Profile", person, SwingConstants.CENTER);
            profile.setHorizontalTextPosition(SwingConstants.CENTER);
            profile.setVerticalTextPosition(SwingConstants.BOTTOM); // text below the icon

            frame.add(user);
            frame.add(avatar);
            frame.add(profile);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
