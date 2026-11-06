package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.8 Adding placeholder text and a header (WinUI4K). */
public class PlaceholderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.8 Placeholder and header (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField name = new WTextField("e.g. John Smith");   // The constructor's string is the placeholder text
            name.setHeader("Name");                             // A header shown above the text field
            frame.add(name);

            frame.setSize(400, 180);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
