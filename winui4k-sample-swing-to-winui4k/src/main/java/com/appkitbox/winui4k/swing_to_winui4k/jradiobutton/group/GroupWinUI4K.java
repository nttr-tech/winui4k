package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.group;

import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 6.2 Choose one with ButtonGroup (WinUI4K). */
public class GroupWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("6.2 Choose one (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WRadioButton small = new WRadioButton("Small");
            WRadioButton medium = new WRadioButton("Medium", true);
            WRadioButton large = new WRadioButton("Large");
            WButtonGroup size = new WButtonGroup();
            size.add(small);
            size.add(medium);
            size.add(large);

            frame.add(small);
            frame.add(medium);
            frame.add(large);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
