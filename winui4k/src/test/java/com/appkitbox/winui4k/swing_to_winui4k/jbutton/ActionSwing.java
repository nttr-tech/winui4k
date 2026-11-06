package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/** 1.8 Sharing the handling of a button and a menu with an Action (Swing). */
public class ActionSwing {
    private static void save() {
        System.out.println("Saved");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.8 Action (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            Action saveAction = new AbstractAction("Save", new ImageIcon("icons/save.png")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    save();
                }
            };
            saveAction.putValue(Action.SHORT_DESCRIPTION, "Saves the file");

            JButton saveButton = new JButton(saveAction);
            JMenuItem saveItem = new JMenuItem(saveAction);

            saveAction.setEnabled(false);   // Both the button and the menu become disabled

            frame.add(saveButton);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
