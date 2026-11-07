package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.listener;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPoint;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WViewport;
import com.appkitbox.winui4k.WinUiUtilities;

/** 15.5 React to scrolling (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("15.5 Listener (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel items = new WPanel();
            for (int i = 1; i <= 50; i++) {
                items.add(new WLabel("Item " + i));
            }
            WScrollPane scroll = new WScrollPane(items);
            WLabel status = new WLabel("Top: 0");

            WViewport viewport = scroll.getViewport();
            viewport.addChangeListener(e -> {
                WPoint p = viewport.getViewPosition();
                status.setText("Top: " + p.getY() + " / " + (int) viewport.getViewSize().getHeight());
            });

            frame.add(scroll, WBorderLayout.CENTER);
            frame.add(status, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // scroll once (after the first layout)
            WinUiUtilities.schedule(500, () -> viewport.setViewPosition(new WPoint(0, 120)));
        });
    }
}
