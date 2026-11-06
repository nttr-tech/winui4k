package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.3 Switching between enabled and disabled (WinUI4K). */
public class EnabledWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.3 Enabled and disabled (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WCheckBox agree = new WCheckBox("I agree to the terms of use");
            WButton next = new WButton("Next");
            next.setEnabled(false);
            agree.addItemListener(e -> next.setEnabled(agree.isSelected()));

            frame.add(agree);
            frame.add(next);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
