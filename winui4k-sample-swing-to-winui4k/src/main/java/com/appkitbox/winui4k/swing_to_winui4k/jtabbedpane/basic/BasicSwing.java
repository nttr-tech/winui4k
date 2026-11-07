package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.basic;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** 18.2 Add tabs (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("18.2 Basic (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("General", new JCheckBox("Start with Windows"));
            tabs.addTab("Files", new ImageIcon("icons/save.png"), new JLabel("Save to: C:\\Documents"), "Where files are saved");
            tabs.addTab("About", new JLabel("Version 1.0"));

            frame.add(tabs, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
