package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.action;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 23.3 Share actions between the toolbar and the menu (WinUI4K). */
public class ActionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("23.3 Toolbar Action (WinUI4K)");
            WLabel status = new WLabel("Ready");

            WAction saveAction = new WAbstractAction("Save", Symbol.SAVE) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    status.setText("Saved");
                }
            };
            saveAction.putValue(WAction.SHORT_DESCRIPTION, "Save the file");
            WAction cutAction = new WAbstractAction("Cut", Symbol.CUT) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    status.setText("Cut");
                }
            };
            cutAction.setEnabled(false);              // disables the toolbar button and the menu item

            WCommandBar toolBar = new WCommandBar();
            toolBar.add(saveAction);                  // creates a WAppBarButton for the action
            toolBar.add(cutAction);

            WMenuBarItem editMenu = new WMenuBarItem("Edit");
            editMenu.add(saveAction);                 // creates a menu item for the same action
            editMenu.add(cutAction);
            WMenuBar menuBar = new WMenuBar();
            menuBar.add(editMenu);
            frame.setJMenuBar(menuBar);

            frame.add(toolBar, WBorderLayout.NORTH);
            frame.add(status, WBorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
