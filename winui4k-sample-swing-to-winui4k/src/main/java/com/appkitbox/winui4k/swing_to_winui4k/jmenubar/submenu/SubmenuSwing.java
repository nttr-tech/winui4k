package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.submenu;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/** 21.4 Icons, separators and submenus (Swing). */
public class SubmenuSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("21.4 Submenu (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JMenu fileMenu = new JMenu("File");
            fileMenu.add(new JMenuItem("New", new ImageIcon("icons/new.png")));
            fileMenu.add(new JMenuItem("Open...", new ImageIcon("icons/open.png")));
            JMenu recent = new JMenu("Open Recent");                  // a JMenu inside a JMenu is a submenu
            recent.add("report.txt");
            recent.add("notes.txt");
            fileMenu.add(recent);
            fileMenu.addSeparator();
            fileMenu.add(new JMenuItem("Save", new ImageIcon("icons/save.png")));

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(fileMenu);
            frame.setJMenuBar(menuBar);

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows its items
            fileMenu.doClick();
        });
    }
}
