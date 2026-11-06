package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.14 Adding a right-click menu (Swing). */
public class PopupSwing {
    private static void renameSelected() {
        System.out.println("Rename");
    }

    private static void deleteSelected() {
        System.out.println("Delete");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.14 Right-click menu (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            JList<String> list = new JList<>(fruits);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JPopupMenu popup = new JPopupMenu();
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> renameSelected());
            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(e -> deleteSelected());
            popup.add(rename);
            popup.addSeparator();
            popup.add(delete);
            list.setComponentPopupMenu(popup);

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);

            frame.setSize(420, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
