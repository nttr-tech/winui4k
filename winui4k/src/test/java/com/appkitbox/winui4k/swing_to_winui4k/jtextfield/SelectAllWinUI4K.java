package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFocusAdapter;
import com.appkitbox.winui4k.WFocusEvent;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.9 Selecting all when the focus arrives (WinUI4K). */
public class SelectAllWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.9 Select all on focus (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField amount = new WTextField(10);
            amount.addFocusListener(new WFocusAdapter() {
                @Override
                public void focusGained(WFocusEvent e) {
                    amount.selectAll();
                }
            });
            amount.setText("12800");
            amount.selectAll();

            frame.add(amount);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
