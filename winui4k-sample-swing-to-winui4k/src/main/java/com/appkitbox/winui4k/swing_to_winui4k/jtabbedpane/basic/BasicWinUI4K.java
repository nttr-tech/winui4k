package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.basic;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WImageIcon;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WinUiUtilities;

/** 18.2 Add tabs (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("18.2 Basic (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTabView tabs = new WTabView();
            tabs.setAddTabButtonVisible(false);     // hide the "+" button that WinUI shows by default
            tabs.addTab("General", new WCheckBox("Start with Windows"));
            tabs.addTab("Files", new WImageIcon("icons/save.png"), new WLabel("Save to: C:\\Documents"), "Where files are saved");
            tabs.addTab("About", new WLabel("Version 1.0"));

            frame.add(tabs, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
