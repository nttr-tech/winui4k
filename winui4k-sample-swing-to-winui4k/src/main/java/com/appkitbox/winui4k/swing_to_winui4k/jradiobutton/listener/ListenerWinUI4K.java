package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.listener;

import com.appkitbox.winui4k.WActionListener;
import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 6.4 React to the selection (WinUI4K). */
public class ListenerWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("6.4 React to the selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel shipping = new WLabel("Shipping: Standard");
            WActionListener listener = e -> shipping.setText("Shipping: " + e.getActionCommand());
            WRadioButton standard = new WRadioButton("Standard", true);
            WRadioButton express = new WRadioButton("Express");
            standard.addActionListener(listener);
            express.addActionListener(listener);
            WButtonGroup group = new WButtonGroup();
            group.add(standard);
            group.add(express);

            frame.add(standard);
            frame.add(express);
            frame.add(shipping);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            express.doClick(); // click "Express" once at startup
        });
    }
}
