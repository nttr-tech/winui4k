package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.basic;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/** 21.2 Add a menu bar and respond to items (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("21.2 Menu Bar (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("Choose a menu item");

            JMenu fileMenu = new JMenu("File");
            JMenuItem newItem = new JMenuItem("New");
            newItem.addActionListener(e -> status.setText("New clicked"));
            JMenuItem openItem = new JMenuItem("Open...");
            openItem.addActionListener(e -> status.setText("Open clicked"));
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(e -> frame.dispose());
            fileMenu.add(newItem);
            fileMenu.add(openItem);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);

            JMenu helpMenu = new JMenu("Help");
            helpMenu.add("About").addActionListener(e -> status.setText("About clicked"));

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows its items
            fileMenu.doClick();
        });
    }
}
