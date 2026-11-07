package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.listener;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/** 21.6 Enable items when the menu opens (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("21.6 Menu Listener (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JTextField field = new JTextField("Hello, menu", 15);

            JMenu editMenu = new JMenu("Edit");
            JMenuItem cut = editMenu.add("Cut");
            cut.addActionListener(e -> field.cut());
            JMenuItem copy = editMenu.add("Copy");
            copy.addActionListener(e -> field.copy());
            JMenuItem selectAll = editMenu.add("Select All");
            selectAll.addActionListener(e -> field.selectAll());
            editMenu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {      // the menu is opening
                    boolean hasSelection = field.getSelectedText() != null;
                    cut.setEnabled(hasSelection);
                    copy.setEnabled(hasSelection);
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(editMenu);
            frame.setJMenuBar(menuBar);
            frame.add(field);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the Edit menu once (nothing is selected, so Cut and Copy are disabled)
            editMenu.doClick();
        });
    }
}
