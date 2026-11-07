package com.appkitbox.winui4k.swing_to_winui4k.jspinner.decimal;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WSpinnerNumberModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 9.4 Decimal steps (WinUI4K). */
public class DecimalWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("9.4 Decimal steps (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSpinner price = new WSpinner(new WSpinnerNumberModel(12.0, 0.0, 100.0, 0.5));
            price.setEditor(new WSpinner.NumberEditor(price, "0.00"));

            frame.add(new WLabel("Price:"));
            frame.add(price);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
