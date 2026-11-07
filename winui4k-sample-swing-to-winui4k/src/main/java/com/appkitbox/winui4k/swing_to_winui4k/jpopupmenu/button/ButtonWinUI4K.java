package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.button;

import com.appkitbox.winui4k.FlyoutPlacement;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WinUiUtilities;

/** 22.5 Open a menu below a button (WinUI4K). */
public class ButtonWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("22.5 Button Menu (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Not exported");

            WMenuFlyout popup = new WMenuFlyout();
            popup.add("Export as PDF").addActionListener(e -> status.setText("PDF"));
            popup.add("Export as CSV").addActionListener(e -> status.setText("CSV"));
            popup.setPlacement(FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT);
            WButton export = new WButton("Export");
            export.setFlyout(popup);                     // the button opens the menu by itself

            frame.add(export);
            frame.add(status);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // click the button once so that the screenshot shows the menu
            WinUiUtilities.schedule(500, () -> export.doClick());
        });
    }
}
