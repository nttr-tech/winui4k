package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.validate;

import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WInputVerifier;
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
            age.setInputVerifier(new WInputVerifier() {
                @Override
                public boolean verify(WComponent input) {
                    return isValidAge(((WTextField) input).getText());   // if false, focus is not released
                }
            });
            age.setText("200");

            frame.add(age);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
