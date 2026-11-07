package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.header;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.7 Header and placeholder (WinUI4K). */
public class HeaderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.7 Header and placeholder (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WComboBox size = new WComboBox(new String[] {"Small", "Medium", "Large"});
            size.setHeader("T-shirt size");
            size.setPlaceholderText("Choose a size");
            size.setSelectedIndex(-1);     // the array constructor selects the first item
            size.setWidth(200);

            frame.add(size);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
