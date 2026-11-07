package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.show;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WToolTip;
import com.appkitbox.winui4k.WinUiUtilities;

/** 29.4 Show a tooltip from the program (WinUI4K). */
public class ShowWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("29.4 Show (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField zip = new WTextField(10);
            zip.setText("12-345");
            WToolTip tip = new WToolTip();
            tip.setText("Enter 7 digits, like 123-4567");
            zip.setToolTip(tip);

            frame.add(zip);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // e.g. after a failed check
            WinUiUtilities.schedule(500, () -> tip.setOpen(true));
        });
    }
}
