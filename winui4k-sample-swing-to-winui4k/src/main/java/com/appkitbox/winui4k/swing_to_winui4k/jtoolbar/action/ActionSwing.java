package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.action;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/** 23.3 Share actions between the toolbar and the menu (Swing). */
public class ActionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("23.3 Toolbar Action (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel status = new JLabel("Ready");

            Action saveAction = new AbstractAction("Save", new ImageIcon("icons/save.png")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    status.setText("Saved");
                }
            };
            saveAction.putValue(Action.SHORT_DESCRIPTION, "Save the file");
            Action cutAction = new AbstractAction("Cut", new ImageIcon("icons/cut.png")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    status.setText("Cut");
                }
            };
            cutAction.setEnabled(false);              // disables the toolbar button and the menu item

            JToolBar toolBar = new JToolBar();
            toolBar.add(saveAction);                  // creates a button for the action
            toolBar.add(cutAction);

            JMenu editMenu = new JMenu("Edit");
            editMenu.add(saveAction);                 // creates a menu item for the same action
            editMenu.add(cutAction);
            JMenuBar menuBar = new JMenuBar();
            menuBar.add(editMenu);
            frame.setJMenuBar(menuBar);

            frame.add(toolBar, BorderLayout.NORTH);
            frame.add(status, BorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
