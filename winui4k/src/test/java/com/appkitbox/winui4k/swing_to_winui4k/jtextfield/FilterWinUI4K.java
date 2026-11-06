package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WAttributeSet;
import com.appkitbox.winui4k.WDocumentFilter;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.5 Restricting the characters that can be entered (WinUI4K). */
public class FilterWinUI4K {
    static boolean isDigits(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.5 Restricting characters (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField zip = new WTextField(8);
            zip.getDocument().setDocumentFilter(new WDocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, WAttributeSet attr) {
                    if (isDigits(string)) super.insertString(fb, offset, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, WAttributeSet attrs) {
                    if (text == null || isDigits(text)) super.replace(fb, offset, length, text, attrs);
                }
            });
            zip.setText("1234567");

            frame.add(zip);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
