package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.twogroups;

import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 6.6 Two groups in the same panel (WinUI4K). */
public class TwoGroupsWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("6.6 Two groups (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WRadioButton small = new WRadioButton("Small", true);
            WRadioButton large = new WRadioButton("Large");
            WButtonGroup size = new WButtonGroup();
            size.add(small);
            size.add(large);

            WRadioButton red = new WRadioButton("Red", true);
            WRadioButton blue = new WRadioButton("Blue");
            WButtonGroup color = new WButtonGroup();
            color.add(red);
            color.add(blue);

            frame.add(small);
            frame.add(large);
            frame.add(red);
            frame.add(blue);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            blue.setSelected(true); // changes only the color group
        });
    }
}
