package com.appkitbox.winui4k.swing_to_winui4k.jframe.menubar;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.5 Menu bar, content, and status line (WinUI4K). */
public class MenuBarWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.5 Menu Bar and Status Line (WinUI4K)");

            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem file = new WMenuBarItem("File");
            file.add("Open...");
            file.add("Exit").addActionListener(e -> frame.dispose());
            menuBar.add(file);
            frame.setJMenuBar(menuBar);   // above the content

            WTextPane text = new WTextPane();
            text.setText("Type here");
            frame.add(text, WBorderLayout.CENTER);
            frame.add(new WLabel("Ready"), WBorderLayout.SOUTH);

            frame.setSize(480, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
