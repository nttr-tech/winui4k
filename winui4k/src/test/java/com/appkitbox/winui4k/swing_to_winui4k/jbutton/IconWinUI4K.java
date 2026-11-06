package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.HorizontalTextPosition;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.VerticalTextPosition;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WImageIcon;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.4 Adding icons (WinUI4K). */
public class IconWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.4 Icons (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton save = new WButton("Save", Symbol.SAVE);                  // A built-in Windows icon
            WButton delete = new WButton(Symbol.DELETE);                      // Icon only
            WButton top = new WButton("Download", new WImageIcon("icons/download.png"));  // An image file
            top.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
            top.setVerticalTextPosition(VerticalTextPosition.BOTTOM);

            frame.add(save);
            frame.add(delete);
            frame.add(top);

            frame.setSize(400, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
