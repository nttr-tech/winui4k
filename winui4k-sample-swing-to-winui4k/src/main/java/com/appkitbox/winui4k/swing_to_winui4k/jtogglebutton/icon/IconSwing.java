package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.icon;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** 7.3 Change the icon while selected (Swing). */
public class IconSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("7.3 Selected icon (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JToggleButton play = new JToggleButton("Play", new ImageIcon("icons/play.png"));
            play.setSelectedIcon(new ImageIcon("icons/pause.png"));
            play.addItemListener(e -> play.setText(play.isSelected() ? "Pause" : "Play"));
            JToggleButton other = new JToggleButton("Play", new ImageIcon("icons/play.png"));
            other.setSelectedIcon(new ImageIcon("icons/pause.png"));

            frame.add(play);
            frame.add(other);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            play.setSelected(true); // select only the first button at startup
        });
    }
}
