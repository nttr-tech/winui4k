package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.component;

import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/** 22.2 Add a context menu to a component (Swing). */
public class ComponentSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("22.2 Context Menu (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel target = new JLabel("Right-click this label");

            JPopupMenu popup = new JPopupMenu();
            JMenuItem cut = popup.add(new JMenuItem("Cut", new ImageIcon("icons/cut.png")));
            cut.addActionListener(e -> target.setText("Cut"));
            JMenuItem copy = popup.add(new JMenuItem("Copy", new ImageIcon("icons/copy.png")));
            copy.addActionListener(e -> target.setText("Copy"));
            popup.addSeparator();
            popup.add("Properties").addActionListener(e -> target.setText("Properties"));
            target.setComponentPopupMenu(popup);

            frame.add(target);

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once so that the screenshot shows it (right-clicking opens it in the same way)
            popup.show(target, 20, 20);
        });
    }
}
