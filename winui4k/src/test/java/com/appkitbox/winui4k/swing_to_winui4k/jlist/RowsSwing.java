package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.10 The number of visible rows and the item height (Swing). */
public class RowsSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.10 Visible rows and height (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry", "Strawberry", "Melon", "Watermelon"};
            JList<String> list = new JList<>(fruits);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            list.setVisibleRowCount(5);
            list.setFixedCellHeight(28);

            frame.add(new JScrollPane(list));

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
