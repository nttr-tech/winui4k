package com.appkitbox.winui4k.swing_to_winui4k.jlist.move;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** 3.12 Move items up and down (Swing). */
public class MoveSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.12 Move Items (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultListModel<String> model = new DefaultListModel<>();
            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            for (String fruit : fruits) {
                model.addElement(fruit);
            }
            JList<String> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JButton up = new JButton("Up");
            up.addActionListener(e -> {
                int index = list.getSelectedIndex();
                if (index <= 0) return;
                String item = model.remove(index);
                model.add(index - 1, item);
                list.setSelectedIndex(index - 1);
                list.ensureIndexIsVisible(index - 1);
            });

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);
            frame.add(up);

            list.setSelectedIndex(2);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
