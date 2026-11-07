package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.listener;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WMenuAdapter;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WMenuEvent;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 21.6 Enable items when the menu opens (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("21.6 Menu Listener (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WTextField field = new WTextField("Hello, menu", 15);

            WMenuBarItem editMenu = new WMenuBarItem("Edit");
            WMenuFlyoutItem cut = editMenu.add("Cut");
            cut.addActionListener(e -> field.cut());
            WMenuFlyoutItem copy = editMenu.add("Copy");
            copy.addActionListener(e -> field.copy());
            WMenuFlyoutItem selectAll = editMenu.add("Select All");
            selectAll.addActionListener(e -> field.selectAll());
            editMenu.addMenuListener(new WMenuAdapter() {
                @Override
                public void menuSelected(WMenuEvent e) {     // the menu is opening
                    boolean hasSelection = field.getSelectedText() != null;
                    cut.setEnabled(hasSelection);
                    copy.setEnabled(hasSelection);
                }
            });

            WMenuBar menuBar = new WMenuBar();
            menuBar.add(editMenu);
            frame.setJMenuBar(menuBar);
            frame.add(field);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the Edit menu once (nothing is selected, so Cut and Copy are disabled)
            editMenu.doClick();
        });
    }
}
