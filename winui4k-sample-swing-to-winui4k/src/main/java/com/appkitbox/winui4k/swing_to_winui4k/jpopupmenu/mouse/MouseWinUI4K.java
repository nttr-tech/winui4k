package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.mouse;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WMouseAdapter;
import com.appkitbox.winui4k.WMouseEvent;
import com.appkitbox.winui4k.WinUiUtilities;

/** 22.3 Open the menu at the mouse position yourself (WinUI4K). */
public class MouseWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("22.3 Popup Trigger (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel target = new WLabel("Right-click this label");

            WMenuFlyout popup = new WMenuFlyout();
            popup.add("Rename").addActionListener(e -> target.setText("Rename"));
            popup.add("Delete").addActionListener(e -> target.setText("Delete"));
            target.addMouseListener(new WMouseAdapter() {
                @Override
                public void mousePressed(WMouseEvent e) {
                    showPopup(e);
                }

                @Override
                public void mouseReleased(WMouseEvent e) {
                    showPopup(e);
                }

                private void showPopup(WMouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            frame.add(target);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once at (60, 10) of the label, as if it were right-clicked there
            popup.show(target, 60, 10);
        });
    }
}
