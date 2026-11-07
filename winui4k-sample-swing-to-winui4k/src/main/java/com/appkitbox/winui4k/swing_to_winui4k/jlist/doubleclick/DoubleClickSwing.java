package com.appkitbox.winui4k.swing_to_winui4k.jlist.doubleclick;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.8 Open on double-click (Swing). */
public class DoubleClickSwing {
    private static void openItem(String item) {
        System.out.println("Open: " + item);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.8 Double Click (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            JList<String> list = new JList<>(fruits);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = list.locationToIndex(e.getPoint());
                        if (index >= 0) openItem(list.getModel().getElementAt(index));
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);
            frame.add(new JLabel("Double-click an item to open it"));

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
