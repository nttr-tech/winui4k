package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.component;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 22.2 Add a context menu to a component (WinUI4K). */
public class ComponentWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("22.2 Context Menu (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel target = new WLabel("Right-click this label");

            WMenuFlyout popup = new WMenuFlyout();
            WMenuFlyoutItem cut = popup.add(new WMenuFlyoutItem("Cut", Symbol.CUT));
            cut.addActionListener(e -> target.setText("Cut"));
            WMenuFlyoutItem copy = popup.add(new WMenuFlyoutItem("Copy", Symbol.COPY));
            copy.addActionListener(e -> target.setText("Copy"));
            popup.addSeparator();
            popup.add("Properties").addActionListener(e -> target.setText("Properties"));
            target.setComponentPopupMenu(popup);

            frame.add(target);

            frame.setSize(400, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once so that the screenshot shows it (right-clicking opens it in the same way)
            WinUiUtilities.schedule(500, () -> popup.show(target, 20, 20));
        });
    }
}
