package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.keys;

import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** 21.3 Mnemonics and shortcut keys (Swing). */
public class KeysSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("21.3 Shortcut Keys (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Press Ctrl+S");

            JMenu fileMenu = new JMenu("File");
            fileMenu.setMnemonic(KeyEvent.VK_F);                       // Alt+F opens the menu
            JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
            save.addActionListener(e -> status.setText("Saved"));
            JMenuItem saveAs = new JMenuItem("Save As...", KeyEvent.VK_A);
            saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                    InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            saveAs.addActionListener(e -> status.setText("Saved as..."));
            fileMenu.add(save);
            fileMenu.add(saveAs);

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(fileMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows the shortcut keys
            fileMenu.doClick();
        });
    }
}
