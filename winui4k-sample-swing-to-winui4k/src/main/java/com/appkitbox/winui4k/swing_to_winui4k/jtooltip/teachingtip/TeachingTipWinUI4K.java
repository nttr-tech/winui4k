package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.teachingtip;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTeachingTip;
import com.appkitbox.winui4k.WinUiUtilities;

/** 29.5 Point out a new feature with a teaching tip (WinUI4K). */
public class TeachingTipWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("29.5 Teaching tip (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel();
            WButton sync = new WButton("Sync", Symbol.SYNC);
            WTeachingTip tip = new WTeachingTip("New: Sync", "Click here to sync your files with the cloud.");
            tip.setTarget(sync);
            tip.setActionButtonText("Try it");
            tip.setCloseButtonText("Got it");
            tip.addActionListener(() -> {
                tip.hide();
                status.setText("Syncing...");
            });

            frame.add(sync);
            frame.add(status);
            frame.add(tip);   // keep it in the window; it draws nothing while closed

            frame.setSize(480, 360);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            tip.show();
        });
    }
}
