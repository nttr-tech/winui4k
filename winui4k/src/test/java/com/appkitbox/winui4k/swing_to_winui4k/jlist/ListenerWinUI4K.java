package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.5 Responding to selection changes (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.5 Responding to selection changes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] fruits = {"Apple", "Orange", "Grape", "Peach", "Cherry"};
            WList list = new WList(fruits);
            list.setWidth(200);
            list.setHeight(150);

            WLabel detail = new WLabel("Please select");
            list.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                Object value = list.getSelectedValue();
                detail.setText(value == null ? "Please select" : "Selected " + value);
            });

            frame.add(list);
            frame.add(detail);

            list.setSelectedIndex(2);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
