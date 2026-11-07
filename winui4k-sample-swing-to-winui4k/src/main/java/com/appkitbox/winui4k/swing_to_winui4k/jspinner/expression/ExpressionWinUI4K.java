package com.appkitbox.winui4k.swing_to_winui4k.jspinner.expression;

import com.appkitbox.winui4k.SpinButtonPlacementMode;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WinUiUtilities;

/** 9.6 Header, placeholder and expressions (WinUI4K). */
public class ExpressionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("9.6 Expressions (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSpinner amount = new WSpinner(Double.NaN);       // empty (NaN) until the user types
            amount.setHeader("Amount");
            amount.setPlaceholderText("Type 12 * 3");
            amount.setAcceptsExpression(true);                // "12 * 3" becomes 36
            amount.setSpinButtonPlacementMode(SpinButtonPlacementMode.COMPACT);
            amount.setWidth(200);

            frame.add(amount);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
