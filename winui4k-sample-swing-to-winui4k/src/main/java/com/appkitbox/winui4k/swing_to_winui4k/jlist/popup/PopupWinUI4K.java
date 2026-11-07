package com.appkitbox.winui4k.swing_to_winui4k.jlist.popup;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.14 Add a context menu (WinUI4K). */
public class PopupWinUI4K {
    private static void renameSelected() {
        System.out.println("Rename");
    }

    private static void deleteSelected() {
        System.out.println("Delete");
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.14 Popup Menu (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);

            WMenuFlyout popup = new WMenuFlyout();
            popup.add("Rename").addActionListener(() -> renameSelected());
            popup.addSeparator();
            popup.add(new WMenuFlyoutItem("Delete", Symbol.DELETE)).addActionListener(() -> deleteSelected());
            list.setComponentPopupMenu(popup);

            list.setWidth(200);
            list.setHeight(150);
            frame.add(list);

            frame.setSize(420, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
