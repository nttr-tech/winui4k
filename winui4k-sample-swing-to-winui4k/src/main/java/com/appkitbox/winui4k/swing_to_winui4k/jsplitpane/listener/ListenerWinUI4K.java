package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.listener;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSplitPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 17.4 React when the divider moves (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("17.4 Listener (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLabel status = new WLabel(" ");
            WSplitPane split = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT,
                    new WLabel("Left"), new WLabel("Right"));
            split.addChangeListener(
                    e -> status.setText("Divider at " + split.getDividerLocation()
                            + " (was " + split.getLastDividerLocation() + ")"));

            frame.add(split, WBorderLayout.CENTER);
            frame.add(status, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            split.setDividerLocation(200);      // move the divider once
        });
    }
}
