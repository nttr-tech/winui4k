package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.keys;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WInputEvent;
import com.appkitbox.winui4k.WKeyEvent;
import com.appkitbox.winui4k.WKeyStroke;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 21.3 Mnemonics and shortcut keys (WinUI4K). */
public class KeysWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("21.3 Shortcut Keys (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Press Ctrl+S");

            WMenuBarItem fileMenu = new WMenuBarItem("File");
            fileMenu.setMnemonic('F');                                 // Alt+F opens the menu
            WMenuFlyoutItem save = new WMenuFlyoutItem("Save", WKeyEvent.VK_S);
            save.setAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK));
            save.addActionListener(e -> status.setText("Saved"));
            WMenuFlyoutItem saveAs = new WMenuFlyoutItem("Save As...", WKeyEvent.VK_A);
            saveAs.setAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_S,
                    WInputEvent.CTRL_DOWN_MASK | WInputEvent.SHIFT_DOWN_MASK));
            saveAs.addActionListener(e -> status.setText("Saved as..."));
            fileMenu.add(save);
            fileMenu.add(saveAs);

            WMenuBar menuBar = new WMenuBar();
            menuBar.add(fileMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the File menu once so that the screenshot shows the shortcut keys
            WinUiUtilities.schedule(500, () -> fileMenu.setPopupMenuVisible(true));
        });
    }
}
