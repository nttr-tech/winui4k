package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.placement;

import com.appkitbox.winui4k.ToolTipPlacement;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WToolTip;
import com.appkitbox.winui4k.WinUiUtilities;

/** 29.2 Choose where the tooltip appears (WinUI4K). */
public class PlacementWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("29.2 Placement (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton save = new WButton("Save");
            WToolTip tip = new WToolTip();
            tip.setText("Save the file (Ctrl+S)");
            tip.setPlacement(ToolTipPlacement.BOTTOM);   // below the button
            save.setToolTip(tip);

            frame.add(save);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // for the screenshot: show the tooltip without the mouse (see 32.4)
            WinUiUtilities.schedule(500, () -> tip.setOpen(true));
        });
    }
}
