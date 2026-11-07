package com.appkitbox.winui4k.swing_to_winui4k.jframe.menubar;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 24.5 Menu bar, content, and status line (Swing). */
public class MenuBarSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("24.5 Menu Bar and Status Line (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JMenuBar menuBar = new JMenuBar();
            JMenu file = new JMenu("File");
            file.add(new JMenuItem("Open..."));
            file.add(new JMenuItem("Exit")).addActionListener(e -> frame.dispose());
            menuBar.add(file);
            frame.setJMenuBar(menuBar);   // above the content pane

            frame.add(new JScrollPane(new JTextArea("Type here")), BorderLayout.CENTER);
            frame.add(new JLabel("Ready"), BorderLayout.SOUTH);

            frame.setSize(480, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
