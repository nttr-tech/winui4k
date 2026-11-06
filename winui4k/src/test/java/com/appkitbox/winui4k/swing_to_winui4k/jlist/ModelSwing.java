package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 3.3 Adding and removing items with a model (Swing). */
public class ModelSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.3 Adding and removing with a model (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("Milk");
            model.addElement("Eggs");
            JList<String> list = new JList<>(model);

            JTextField input = new JTextField(15);
            JButton add = new JButton("Add");
            add.addActionListener(e -> {
                if (!input.getText().isEmpty()) {
                    model.addElement(input.getText());
                    input.setText("");
                }
            });

            JButton remove = new JButton("Remove");
            remove.addActionListener(e -> {
                int index = list.getSelectedIndex();
                if (index >= 0) model.remove(index);
            });

            JButton clear = new JButton("Remove all");
            clear.addActionListener(e -> model.clear());

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);
            frame.add(input);
            frame.add(add);
            frame.add(remove);
            frame.add(clear);

            frame.setSize(520, 360);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
