package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.listener;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WPopupMenuAdapter;
import com.appkitbox.winui4k.WPopupMenuEvent;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.ArrayList;
import java.util.List;

/** 22.4 Update items just before the menu opens (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("22.4 Popup Menu Listener (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel target = new WLabel("Right-click this label");
            List<String> history = new ArrayList<>();
            history.add("Typing");

            WMenuFlyout popup = new WMenuFlyout();
            WMenuFlyoutItem undo = popup.add("Undo");
            undo.addActionListener(e -> history.remove(history.size() - 1));
            popup.add("Select All");
            popup.addPopupMenuListener(new WPopupMenuAdapter() {
                @Override
                public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                    undo.setEnabled(!history.isEmpty());
                    undo.setText(history.isEmpty() ? "Undo" : "Undo " + history.get(history.size() - 1));
                }
            });
            target.setComponentPopupMenu(popup);

            frame.add(target);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once so that the screenshot shows the updated item
            popup.show(target, 20, 20);
        });
    }
}
