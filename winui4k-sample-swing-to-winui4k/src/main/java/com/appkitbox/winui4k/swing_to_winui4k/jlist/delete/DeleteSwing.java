package com.appkitbox.winui4k.swing_to_winui4k.jlist.delete;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** 3.11 Delete the selected item with the Delete key (Swing). */
public class DeleteSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.11 Delete Key (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultListModel<String> model = new DefaultListModel<>();
            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            for (String fruit : fruits) {
                model.addElement(fruit);
            }
            JList<String> list = new JList<>(model);

            list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
            list.getActionMap().put("delete", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = list.getSelectedIndex();
                    if (index >= 0) model.remove(index);
                }
            });

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);
            frame.add(new JLabel("Select an item and press Delete to remove it"));

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
