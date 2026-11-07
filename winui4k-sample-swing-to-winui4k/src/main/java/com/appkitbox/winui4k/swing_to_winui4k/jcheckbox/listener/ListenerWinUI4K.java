package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.listener;

import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WItemEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 5.3 React to check changes (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("5.3 React to changes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WCheckBox wifi = new WCheckBox("Wi-Fi");
            WLabel state = new WLabel("Wi-Fi is off");
            wifi.addItemListener(e -> state.setText(
                    e.getStateChange() == WItemEvent.SELECTED ? "Wi-Fi is on" : "Wi-Fi is off"));

            frame.add(wifi);
            frame.add(state);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            wifi.setSelected(true); // the listener is also called for programmatic changes
        });
    }
}
