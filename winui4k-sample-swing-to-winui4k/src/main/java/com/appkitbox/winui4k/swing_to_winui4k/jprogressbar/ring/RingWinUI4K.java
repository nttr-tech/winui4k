package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.ring;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WProgressRing;
import com.appkitbox.winui4k.WinUiUtilities;

/** 11.7 Progress ring, paused and error states (WinUI4K). */
public class RingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.7 Ring and states (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressRing ring = new WProgressRing();         // spins until setActive(false)

            WProgressBar paused = new WProgressBar(0, 100);
            paused.setValue(60);
            paused.setShowPaused(true);

            WProgressBar error = new WProgressBar(0, 100);
            error.setValue(80);
            error.setShowError(true);

            frame.add(ring);
            frame.add(paused);
            frame.add(error);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
