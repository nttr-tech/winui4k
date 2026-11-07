package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 21.2 Add a menu bar and respond to items (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("21.2 Menu Bar (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("Choose a menu item");

            WMenuBarItem fileMenu = new WMenuBarItem("File");
            WMenuFlyoutItem newItem = new WMenuFlyoutItem("New");
            newItem.addActionListener(e -> status.setText("New clicked"));
            WMenuFlyoutItem openItem = new WMenuFlyoutItem("Open...");
            openItem.addActionListener(e -> status.setText("Open clicked"));
            WMenuFlyoutItem exitItem = new WMenuFlyoutItem("Exit");
            exitItem.addActionListener(e -> frame.dispose());
            fileMenu.add(newItem);
            fileMenu.add(openItem);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);

            WMenuBarItem helpMenu = new WMenuBarItem("Help");
            helpMenu.add("About").addActionListener(e -> status.setText("About clicked"));

            WMenuBar menuBar = new WMenuBar();
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows its items
            fileMenu.doClick();
        });
    }
}
