package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.7 Selecting and scrolling from the program (Swing). */
public class SelectSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.7 Selecting from the program (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] items = new String[50];
            for (int i = 0; i < items.length; i++) {
                items[i] = "Item " + (i + 1);
            }
            JList<String> list = new JList<>(items);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            JLabel result = new JLabel("Selected item: none");

            frame.add(scroll);
            frame.add(result);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            list.setSelectedIndex(42);
            list.ensureIndexIsVisible(42);
            result.setText("Selected item: " + list.getSelectedValue());
        });
    }
}
