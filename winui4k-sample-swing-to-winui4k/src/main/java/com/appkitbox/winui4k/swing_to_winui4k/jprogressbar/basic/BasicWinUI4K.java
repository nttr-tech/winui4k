package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WinUiUtilities;

/** 11.2 Show progress (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.2 Progress (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressBar progress = new WProgressBar(0, 100);
            progress.setValue(40);

            frame.add(new WLabel("Copying files..."));
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
