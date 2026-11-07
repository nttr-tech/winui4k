package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.indeterminate;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WinUiUtilities;

/** 11.3 Progress with no known end (WinUI4K). */
public class IndeterminateWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.3 Indeterminate (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressBar progress = new WProgressBar();
            progress.setIndeterminate(true);

            frame.add(new WLabel("Connecting..."));
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
