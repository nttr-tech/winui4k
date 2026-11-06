package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.11 Working with the selection and the caret (WinUI4K). */
public class CaretWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.11 Selection and caret (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel position = new WLabel("Position: 0");
            WTextField text = new WTextField(30);
            text.setText("Hello, World");
            text.select(7, 12);
            String selected = text.getSelectedText();
            System.out.println("Selected text: " + selected);
            text.replaceSelection("WinUI");              // "Hello, WinUI"
            text.setCaretPosition(text.getText().length());
            text.addCaretListener(e -> position.setText("Position: " + e.getDot()));
            position.setText("Position: " + text.getCaretPosition());
            text.select(7, 12);                          // Leave the replaced word selected

            frame.add(text);
            frame.add(position);

            frame.setSize(560, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
