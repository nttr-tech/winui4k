package com.appkitbox.winui4k.swing_to_winui4k.jspinner.basic;

import com.appkitbox.winui4k.SpinButtonPlacementMode;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WSpinnerNumberModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 9.2 Number spinner (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("9.2 Number spinner (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSpinner quantity = new WSpinner(new WSpinnerNumberModel(1, 0, 10, 1));
            quantity.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);   // show the up and down buttons

            frame.add(new WLabel("Quantity:"));
            frame.add(quantity);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
