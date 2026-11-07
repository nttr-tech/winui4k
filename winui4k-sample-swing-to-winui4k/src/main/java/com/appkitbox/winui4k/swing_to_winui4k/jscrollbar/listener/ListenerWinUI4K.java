package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.listener;

import com.appkitbox.winui4k.WAdjustable;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 16.3 Read the scroll position as a scroll bar value (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("16.3 Listener (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel items = new WPanel();
            for (int i = 1; i <= 50; i++) {
                items.add(new WLabel("Item " + i));
            }
            WScrollPane scroll = new WScrollPane(items);
            WLabel status = new WLabel(" ");

            WAdjustable bar = scroll.getVerticalScrollBar();
            bar.addAdjustmentListener(e -> {
                int percent = 100 * e.getValue() / (bar.getMaximum() - bar.getVisibleAmount());
                status.setText("Value " + e.getValue() + " of " + bar.getMaximum()
                        + " (visible " + bar.getVisibleAmount() + ", " + percent + "%)");
            });

            frame.add(scroll, WBorderLayout.CENTER);
            frame.add(status, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            WinUiUtilities.schedule(500, () -> bar.setValue(300));     // scroll once (after the first layout)
        });
    }
}
