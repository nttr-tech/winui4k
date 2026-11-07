package com.appkitbox.winui4k.swing_to_winui4k.jspinner.listener;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WSpinnerNumberModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 9.3 Read the value and react to changes (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("9.3 Value changes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSpinner age = new WSpinner(new WSpinnerNumberModel(20, 0, 120, 1));
            WLabel result = new WLabel("Age: 20");
            age.addChangeListener(e -> {
                int value = (int) age.getValue();
                result.setText("Age: " + value);
            });

            frame.add(age);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            age.setValue(35);
        });
    }
}
