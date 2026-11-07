package com.appkitbox.winui4k.swing_to_winui4k.jframe.titlebar;

import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.SystemBackdropType;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTitleBar;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.7 Custom title bar and backdrop (WinUI4K). */
public class TitleBarWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.7 Title Bar (WinUI4K)");

            // a title bar with a search box, drawn by the app
            WTitleBar titleBar = new WTitleBar();
            titleBar.setTitle("24.7 Title Bar (WinUI4K)");
            titleBar.setSubtitle("Preview");
            WTextField search = new WTextField();
            search.setPlaceholderText("Search");
            search.setWidth(200);
            titleBar.setContent(search);

            WGrid root = new WGrid();
            root.addRow(GridLength.AUTO);
            root.addRow(GridLength.star());
            root.add(titleBar, 0, 0);
            root.add(new WLabel("The content starts right below the title bar"), 1, 0);

            frame.setContentPane(root);
            frame.setExtendsContentIntoTitleBar(true);
            frame.setTitleBar(titleBar);
            frame.setSystemBackdrop(SystemBackdropType.MICA_ALT);

            frame.setSize(720, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
