package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.placeholder;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.8 Add placeholder text and a header (WinUI4K). */
public class PlaceholderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.8 Placeholder and Header (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField name = new WTextField(20);
            name.setPlaceholderText("e.g. John Smith");      // shown while the field is empty
            name.setHeader("Name");                             // header shown above the field
            frame.add(name);

            frame.setSize(400, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
