package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.basic;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WSplitPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 17.2 Split the window into left and right (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("17.2 Basic (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WList files = new WList(new String[] {"readme.txt", "build.gradle", "Main.java", "notes.md"});
            WLabel preview = new WLabel("Select a file");
            WSplitPane split = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT, files, preview);   // WList scrolls by itself
            split.setDividerLocation(150);

            frame.add(split, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
