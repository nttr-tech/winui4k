package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.overflow;

import com.appkitbox.winui4k.CommandBarDefaultLabelPosition;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAppBarButton;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 23.6 Label position and the overflow menu (WinUI4K). */
public class OverflowWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("23.6 Overflow Menu (WinUI4K)");
            WLabel status = new WLabel("Ready");

            WCommandBar toolBar = new WCommandBar();
            toolBar.setDefaultLabelPosition(CommandBarDefaultLabelPosition.RIGHT);   // labels to the right of the icons
            WAppBarButton add = new WAppBarButton("Add", Symbol.ADD);
            add.addActionListener(e -> status.setText("Add"));
            WAppBarButton edit = new WAppBarButton("Edit", Symbol.EDIT);
            edit.addActionListener(e -> status.setText("Edit"));
            toolBar.add(add);
            toolBar.add(edit);

            // commands in the "..." menu at the right end of the bar
            WAppBarButton settings = new WAppBarButton("Settings", Symbol.SETTING);
            settings.addActionListener(e -> status.setText("Settings"));
            WAppBarButton help = new WAppBarButton("Help", Symbol.HELP);
            help.addActionListener(e -> status.setText("Help"));
            toolBar.addSecondaryCommand(settings);
            toolBar.addSecondaryCommand(help);

            frame.add(toolBar, WBorderLayout.NORTH);
            frame.add(status, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the "..." menu once so that the screenshot shows it
            toolBar.setOpen(true);
        });
    }
}
