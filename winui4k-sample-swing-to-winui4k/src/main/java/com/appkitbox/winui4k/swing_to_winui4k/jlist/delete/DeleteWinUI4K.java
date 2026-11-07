package com.appkitbox.winui4k.swing_to_winui4k.jlist.delete;

import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WDefaultListModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WKeyEvent;
import com.appkitbox.winui4k.WKeyStroke;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.11 Delete the selected item with the Delete key (WinUI4K). */
public class DeleteWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.11 Delete Key (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WDefaultListModel<String> model = new WDefaultListModel<>();
            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            for (String fruit : fruits) {
                model.addElement(fruit);
            }
            WList list = new WList(model);

            list.getInputMap().put(WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, 0), "delete");
            list.getActionMap().put("delete", new WAbstractAction() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    int index = list.getSelectedIndex();
                    if (index >= 0) model.remove(index);
                }
            });

            list.setWidth(200);
            list.setHeight(150);
            frame.add(list);
            frame.add(new WLabel("Select an item and press Delete to remove it"));

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
