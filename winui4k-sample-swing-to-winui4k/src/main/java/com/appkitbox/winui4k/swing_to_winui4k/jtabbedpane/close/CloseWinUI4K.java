package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.close;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WTabViewItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 18.4 Close a tab with its close button (WinUI4K). */
public class CloseWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("18.4 Close (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTabView tabs = new WTabView();
            for (String name : new String[] {"a.txt", "b.txt", "c.txt"}) {
                WTabViewItem tab = new WTabViewItem(name);  // has a close button by default
                tab.setContent(new WLabel("Contents of " + name));
                tabs.addTab(tab);
            }
            tabs.addTabCloseRequestedListener(index -> tabs.removeTabAt(index));

            frame.add(tabs, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            tabs.removeTabAt(1);    // close "b.txt"
        });
    }
}
