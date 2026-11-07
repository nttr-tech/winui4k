package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.string;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WinUiUtilities;

/** 11.4 Show the percentage (WinUI4K). */
public class StringWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.4 Percentage (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressBar progress = new WProgressBar(0, 100);
            progress.setValue(65);
            progress.setWidth(200);   // a progress bar has no default width
            progress.setStringPainted(true);       // only tells screen readers; nothing is drawn
            WLabel percent = new WLabel(progress.getString());
            progress.addChangeListener(e -> percent.setText(progress.getString()));

            frame.add(progress);
            frame.add(percent);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
