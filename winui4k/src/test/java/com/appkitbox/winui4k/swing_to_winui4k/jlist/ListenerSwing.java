package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.5 Responding to selection changes (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.5 Responding to selection changes (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            JList<String> list = new JList<>(fruits);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));

            JLabel detail = new JLabel("Please select");
            list.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;          // Skip the intermediate states while dragging
                String value = list.getSelectedValue();
                detail.setText(value == null ? "Please select" : "Selected " + value);
            });

            frame.add(scroll);
            frame.add(detail);

            list.setSelectedIndex(2);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
