package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.validate;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFocusAdapter;
import com.appkitbox.winui4k.WFocusEvent;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.10 Validate when focus is lost (WinUI4K). */
public class ValidateWinUI4K {
    static boolean isValidAge(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return 0 <= value && value <= 150;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.10 Validate on Focus Lost (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField age = new WTextField(4);
            WLabel error = new WLabel();
            error.setForeground(WColor.RED);
            age.addFocusListener(new WFocusAdapter() {
                @Override
                public void focusLost(WFocusEvent e) {
                    error.setText(isValidAge(age.getText()) ? "" : "Enter a number from 0 to 150");
                }
            });
            age.setText("200");
            // show the message at startup (evaluate the same expression as focusLost once)
            error.setText(isValidAge(age.getText()) ? "" : "Enter a number from 0 to 150");

            frame.add(age);
            frame.add(error);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
