package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.togglebuttonswitch;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WToggleSwitch;
import com.appkitbox.winui4k.WinUiUtilities;

/** 7.6 Toggle switch (WinUI4K). */
public class SwitchWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("7.6 Toggle switch (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            // An on/off switch for settings (the header is shown above the switch)
            WLabel status = new WLabel("Wi-Fi is off");
            WToggleSwitch wifi = new WToggleSwitch("Wi-Fi");
            wifi.setOnContent("Connected");
            wifi.setOffContent("Disconnected");
            wifi.addItemListener(e -> status.setText(wifi.isOn() ? "Wi-Fi is on" : "Wi-Fi is off"));

            frame.add(wifi);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            wifi.setOn(true); // turn it on once at startup
        });
    }
}
