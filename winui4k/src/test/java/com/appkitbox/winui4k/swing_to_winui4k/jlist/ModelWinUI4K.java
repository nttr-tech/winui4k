package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WDefaultListModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.3 Adding and removing items with a model (WinUI4K). */
public class ModelWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.3 Adding and removing with a model (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WDefaultListModel<String> model = new WDefaultListModel<>();
            model.addElement("Milk");
            model.addElement("Eggs");
            WList list = new WList(model);

            WTextField input = new WTextField(15);
            WButton add = new WButton("Add");
            add.addActionListener(e -> {
                if (!input.getText().isEmpty()) {
                    model.addElement(input.getText());
                    input.setText("");
                }
            });

            WButton remove = new WButton("Remove");
            remove.addActionListener(e -> {
                int index = list.getSelectedIndex();
                if (index >= 0) model.remove(index);
            });

            WButton clear = new WButton("Remove all");
            clear.addActionListener(e -> model.clear());

            list.setWidth(200);
            list.setHeight(150);
            WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
            row.add(input);
            row.add(add);
            row.add(remove);
            row.add(clear);
            frame.add(list);
            frame.add(row);

            frame.setSize(520, 360);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
