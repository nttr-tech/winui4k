package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.state;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WKeyEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WinUiUtilities;

/** 18.5 Disable a tab, rename it, and give it a mnemonic (WinUI4K). */
public class StateWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("18.5 State (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTabView tabs = new WTabView();
            tabs.setAddTabButtonVisible(false);
            tabs.addTab("Document", new WLabel("Hello"));
            tabs.addTab("Preview", new WLabel("Preview"));
            tabs.addTab("Publish", new WLabel("Publish"));

            tabs.setTitleAt(0, "Document *");           // mark as modified
            tabs.setEnabledAt(2, false);                // cannot publish yet
            tabs.setToolTipTextAt(2, "Save the document first");
            tabs.setMnemonicAt(1, WKeyEvent.VK_P);      // Alt+P selects "Preview"

            frame.add(tabs, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
