package com.appkitbox.winui4k.swing_to_winui4k.jlabel.wrap;

import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.7 Wrap long text (WinUI4K). */
public class WrapWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.7 Wrap long text (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String message = "Your changes have not been saved. "
                    + "If you close the window now, the changes will be lost.";
            WLabel wrapped = new WLabel(message);
            wrapped.setTextWrapping(TextWrapping.WRAP);
            wrapped.setWidth(250);

            frame.add(wrapped);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
