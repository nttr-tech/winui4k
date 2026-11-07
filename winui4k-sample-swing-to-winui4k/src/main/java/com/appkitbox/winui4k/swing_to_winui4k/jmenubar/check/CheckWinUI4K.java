package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.check;

import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WRadioMenuFlyoutItem;
import com.appkitbox.winui4k.WToggleMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 21.5 Check box and radio button menu items (WinUI4K). */
public class CheckWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("21.5 Check and Radio Items (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Zoom: Medium");

            WMenuBarItem viewMenu = new WMenuBarItem("View");
            WToggleMenuFlyoutItem statusBar = new WToggleMenuFlyoutItem("Status Bar", true);
            statusBar.addItemListener(e -> status.setVisible(statusBar.isSelected()));
            WToggleMenuFlyoutItem wordWrap = new WToggleMenuFlyoutItem("Word Wrap");
            viewMenu.add(statusBar);
            viewMenu.add(wordWrap);
            viewMenu.addSeparator();

            WButtonGroup zoom = new WButtonGroup();
            for (String size : new String[] {"Small", "Medium", "Large"}) {
                WRadioMenuFlyoutItem item = new WRadioMenuFlyoutItem(size, size.equals("Medium"));
                item.addActionListener(e -> status.setText("Zoom: " + e.getActionCommand()));
                zoom.add(item);
                viewMenu.add(item);
            }

            WMenuBar menuBar = new WMenuBar();
            menuBar.add(viewMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the View menu once so that the screenshot shows the check marks
            viewMenu.doClick();
        });
    }
}
