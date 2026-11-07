package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.splitview;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SplitViewDisplayMode;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WSplitView;
import com.appkitbox.winui4k.WinUiUtilities;

/** 17.6 A pane that opens and closes (WinUI4K). */
public class SplitViewWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("17.6 SplitView (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel menu = new WPanel(4.0, Orientation.VERTICAL);
            menu.add(new WLabel("Home"));
            menu.add(new WLabel("Settings"));
            WButton toggle = new WButton("Menu");

            WSplitView view = new WSplitView(menu, toggle);
            view.setDisplayMode(SplitViewDisplayMode.INLINE);   // push the content aside
            view.setOpenPaneLength(160);
            toggle.addActionListener(e -> view.setPaneOpen(!view.isPaneOpen()));

            frame.add(view, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            view.setPaneOpen(true);     // open the pane once
        });
    }
}
