package com.appkitbox.winui4k.swing_to_winui4k.jlist.move;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WDefaultListModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WListSelectionModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.12 Move items up and down (WinUI4K). */
public class MoveWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.12 Move Items (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WDefaultListModel<String> model = new WDefaultListModel<>();
            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            for (String fruit : fruits) {
                model.addElement(fruit);
            }
            WList list = new WList(model);
            list.setSelectionMode(WListSelectionModel.SINGLE_SELECTION);

            WButton up = new WButton("Up", Symbol.UP);
            up.addActionListener(e -> {
                int index = list.getSelectedIndex();
                if (index <= 0) return;
                String item = model.remove(index);
                model.add(index - 1, item);
                list.setSelectedIndex(index - 1);
                list.ensureIndexIsVisible(index - 1);
            });

            list.setWidth(200);
            list.setHeight(150);
            frame.add(list);
            frame.add(up);

            list.setSelectedIndex(2);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
