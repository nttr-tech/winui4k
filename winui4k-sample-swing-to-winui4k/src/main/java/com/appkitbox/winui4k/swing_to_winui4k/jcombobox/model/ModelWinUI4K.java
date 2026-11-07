package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.model;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WDefaultComboBoxModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.4 Add and remove items with a model (WinUI4K). */
public class ModelWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.4 Model (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WDefaultComboBoxModel<String> model = new WDefaultComboBoxModel<>(new String[] {"Tokyo", "Osaka"});
            WComboBox city = new WComboBox(model);
            WTextField input = new WTextField(8);
            WButton add = new WButton("Add");
            add.addActionListener(e -> {
                model.addElement(input.getText());
                model.setSelectedItem(input.getText());
            });
            WButton remove = new WButton("Remove");
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
