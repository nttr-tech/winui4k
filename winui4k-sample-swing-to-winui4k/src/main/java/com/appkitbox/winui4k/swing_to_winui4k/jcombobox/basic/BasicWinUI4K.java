package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.basic;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.2 Choose from fixed items (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.2 Fixed items (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WComboBox size = new WComboBox(new String[] {"Small", "Medium", "Large"});
            size.setSelectedIndex(1);
            WLabel result = new WLabel("Selected: " + size.getSelectedItem());

            frame.add(size);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
