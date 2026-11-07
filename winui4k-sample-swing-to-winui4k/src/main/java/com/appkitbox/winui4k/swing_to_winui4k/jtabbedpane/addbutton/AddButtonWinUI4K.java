package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.addbutton;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WTabViewItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 18.6 Browser-like tabs with a "+" button (WinUI4K). */
public class AddButtonWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("18.6 AddButton (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTabView tabs = new WTabView();     // the "+" button appears when a listener is added
            Runnable newTab = () -> {
                WTabViewItem tab = new WTabViewItem("Untitled " + (tabs.getTabCount() + 1));
                tab.setContent(new WLabel("New document"));
                tabs.addTab(tab);
                tabs.setSelectedIndex(tabs.getTabCount() - 1);
            };
            tabs.addAddTabButtonClickListener(newTab);
            tabs.addTabCloseRequestedListener(index -> tabs.removeTabAt(index));

            frame.add(tabs, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            newTab.run();       // open two tabs
            newTab.run();
        });
    }
}
