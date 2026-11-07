package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.model;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 8.4 Add and remove items with a model (Swing). */
public class ModelSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("8.4 Model (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(new String[] {"Tokyo", "Osaka"});
            JComboBox<String> city = new JComboBox<>(model);
            JTextField input = new JTextField(8);
            JButton add = new JButton("Add");
            add.addActionListener(e -> {
                model.addElement(input.getText());
                model.setSelectedItem(input.getText());
            });
            JButton remove = new JButton("Remove");
            remove.addActionListener(e -> model.removeElement(model.getSelectedItem()));

            frame.add(city);
            frame.add(input);
            frame.add(add);
            frame.add(remove);

            frame.setSize(520, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            input.setText("Nagoya");
            add.doClick();
        });
    }
}
