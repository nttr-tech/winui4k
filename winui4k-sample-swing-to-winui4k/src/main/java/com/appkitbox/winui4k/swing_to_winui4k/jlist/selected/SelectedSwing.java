package com.appkitbox.winui4k.swing_to_winui4k.jlist.selected;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.4 Read the selected item (Swing). */
public class SelectedSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.4 Read Selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            JList<String> list = new JList<>(fruits);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            JLabel result = new JLabel();

            list.setSelectedIndex(1);
            String value = list.getSelectedValue();          // null if nothing is selected
            int index = list.getSelectedIndex();             // -1 if nothing is selected
            boolean none = list.isSelectionEmpty();
            result.setText("Selected: " + value + " / index: " + index + " / none: " + none);

            frame.add(scroll);
            frame.add(result);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
