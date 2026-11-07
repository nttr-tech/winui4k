package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.selected;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 6.3 Read the selected button (WinUI4K). */
public class SelectedWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("6.3 Read the selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WRadioButton card = new WRadioButton("Credit card", true);
            WRadioButton bank = new WRadioButton("Bank transfer");
            card.setActionCommand("card");
            bank.setActionCommand("bank");
            WButtonGroup payment = new WButtonGroup();
            payment.add(card);
            payment.add(bank);

            WLabel result = new WLabel();
            WButton ok = new WButton("OK");
            ok.addActionListener(e -> result.setText("Payment: " + payment.getSelection().getActionCommand()));

            frame.add(card);
            frame.add(bank);
            frame.add(ok);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            bank.setSelected(true); // choose "Bank transfer" and press OK once at startup
            ok.doClick();
        });
    }
}
