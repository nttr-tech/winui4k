package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.listener;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WItemEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.3 React to selection changes (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.3 Selection changes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WComboBox color = new WComboBox(new String[] {"Red", "Green", "Blue"});
            WLabel log = new WLabel("No change yet");
            color.addItemListener(e -> {
                if (e.getStateChange() == WItemEvent.SELECTED) {
                    log.setText("Selected: " + e.getItem());
                }
            });

            frame.add(color);
            frame.add(log);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            color.setSelectedItem("Blue");
        });
    }
}
