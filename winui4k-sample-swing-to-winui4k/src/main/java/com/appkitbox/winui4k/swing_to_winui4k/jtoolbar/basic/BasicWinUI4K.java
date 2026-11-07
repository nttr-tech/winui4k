package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.basic;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAppBarButton;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 23.2 A toolbar with buttons (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("23.2 Toolbar (WinUI4K)");
            WLabel status = new WLabel("Click a toolbar button");

            WAppBarButton newButton = new WAppBarButton("New", Symbol.DOCUMENT);
            newButton.setToolTipText("New file");
            newButton.addActionListener(e -> status.setText("New"));
            WAppBarButton openButton = new WAppBarButton("Open", Symbol.OPEN_FILE);
            openButton.setToolTipText("Open a file");
            openButton.addActionListener(e -> status.setText("Open"));
            WAppBarButton saveButton = new WAppBarButton("Save", Symbol.SAVE);
            saveButton.setToolTipText("Save the file");
            saveButton.addActionListener(e -> status.setText("Save"));

            WCommandBar toolBar = new WCommandBar();
            toolBar.add(newButton);
            toolBar.add(openButton);
            toolBar.add(saveButton);

            frame.add(toolBar, WBorderLayout.NORTH);
            frame.add(status, WBorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
