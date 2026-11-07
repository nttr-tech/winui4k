package com.appkitbox.winui4k.swing_to_winui4k.jlabel.alignment;

import com.appkitbox.winui4k.TextAlignment;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSize;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.5 Text alignment (WinUI4K). */
public class AlignmentWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.5 Text alignment (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel left = new WLabel("Left");                      // LEFT by default
            WLabel center = new WLabel("Center");
            center.setTextAlignment(TextAlignment.CENTER);
            WLabel right = new WLabel("Right");
            right.setTextAlignment(TextAlignment.RIGHT);
            for (WLabel label : new WLabel[] {left, center, right}) {
                label.setPreferredSize(new WSize(300, 24));
            }

            frame.add(left);
            frame.add(center);
            frame.add(right);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
