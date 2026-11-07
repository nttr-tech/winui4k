package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.submenu;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WMenuFlyoutSubItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 21.4 Icons, separators and submenus (WinUI4K). */
public class SubmenuWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("21.4 Submenu (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WMenuBarItem fileMenu = new WMenuBarItem("File");
            fileMenu.add(new WMenuFlyoutItem("New", Symbol.DOCUMENT));
            fileMenu.add(new WMenuFlyoutItem("Open...", Symbol.OPEN_FILE));
            WMenuFlyoutSubItem recent = new WMenuFlyoutSubItem("Open Recent");  // a submenu is a separate class
            recent.add("report.txt");
            recent.add("notes.txt");
            fileMenu.add(recent);
            fileMenu.addSeparator();
            fileMenu.add(new WMenuFlyoutItem("Save", Symbol.SAVE));

            WMenuBar menuBar = new WMenuBar();
            menuBar.add(fileMenu);
            frame.setJMenuBar(menuBar);

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows its items
            fileMenu.doClick();
        });
    }
}
