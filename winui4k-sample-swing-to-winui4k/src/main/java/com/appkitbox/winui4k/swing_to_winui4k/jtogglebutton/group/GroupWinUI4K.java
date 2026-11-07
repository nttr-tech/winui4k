package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.group;

import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 7.4 Press only one with ButtonGroup (WinUI4K). */
public class GroupWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("7.4 Press only one (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WToggleButton left = new WToggleButton("Left", true);
            WToggleButton center = new WToggleButton("Center");
            WToggleButton right = new WToggleButton("Right");
            WButtonGroup alignment = new WButtonGroup();
            alignment.add(left);
            alignment.add(center);
            alignment.add(right);

            frame.add(left);
            frame.add(center);
            frame.add(right);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            center.doClick(); // pressing "Center" releases "Left"
        });
    }
}
