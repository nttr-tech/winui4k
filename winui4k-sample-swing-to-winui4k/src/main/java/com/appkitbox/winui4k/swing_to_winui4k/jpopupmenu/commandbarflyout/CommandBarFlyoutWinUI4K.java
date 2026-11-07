package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.commandbarflyout;

import com.appkitbox.winui4k.FlyoutPlacement;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAppBarButton;
import com.appkitbox.winui4k.WCommandBarFlyout;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 22.6 Context menu with a mini toolbar (WinUI4K). */
public class CommandBarFlyoutWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("22.6 Command Bar Flyout (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel target = new WLabel("Right-click this label");

            WCommandBarFlyout flyout = new WCommandBarFlyout();
            WAppBarButton cut = new WAppBarButton("Cut", Symbol.CUT);
            cut.addActionListener(e -> target.setText("Cut"));
            WAppBarButton copy = new WAppBarButton("Copy", Symbol.COPY);
            copy.addActionListener(e -> target.setText("Copy"));
            WAppBarButton paste = new WAppBarButton("Paste", Symbol.PASTE);
            paste.addActionListener(e -> target.setText("Paste"));
            flyout.addPrimaryCommand(cut);             // icons in a row at the top
            flyout.addPrimaryCommand(copy);
            flyout.addPrimaryCommand(paste);
            WAppBarButton selectAll = new WAppBarButton("Select All", Symbol.SELECT_ALL);
            selectAll.addActionListener(e -> target.setText("Select All"));
            flyout.addSecondaryCommand(selectAll);     // menu items below the row
            target.setContextFlyout(flyout);

            frame.add(target);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the flyout once below the label so that the screenshot shows it
            flyout.setPlacement(FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT);
            WinUiUtilities.schedule(500, () -> flyout.show(target, 20, 20));
        });
    }
}
