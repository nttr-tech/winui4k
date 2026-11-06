package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WMouseAdapter;
import com.appkitbox.winui4k.WMouseEvent;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.8 Opening with a double click (WinUI4K). */
public class DoubleClickWinUI4K {
    private static void openItem(Object item) {
        System.out.println("Open: " + item);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.8 Opening with a double click (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);
            list.addMouseListener(new WMouseAdapter() {
                @Override
                public void mouseClicked(WMouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = list.locationToIndex(e.getPoint());
                        if (index >= 0) openItem(list.getModel().getElementAt(index));
                    }
                }
            });

            list.setWidth(200);
            list.setHeight(150);
            frame.add(list);
            frame.add(new WLabel("Double-click an item to open it"));

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
