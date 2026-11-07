package com.appkitbox.winui4k.swing_to_winui4k.jdialog.validate;

import com.appkitbox.winui4k.ContentDialogButton;
import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 25.3 Close the dialog only when the input is valid (WinUI4K). */
public class ValidateWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("25.3 Validate Input (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel(" ");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            WTextField age = new WTextField(8);
            age.setText("abc");
            WLabel error = new WLabel(" ");
            error.setForeground(WColor.RED);
            WContentDialog dialog = new WContentDialog(frame, "Age", true);
            dialog.add(new WLabel("Your age:"));
            dialog.add(age);
            dialog.add(error);
            dialog.setPrimaryButtonText("OK");
            dialog.setDefaultButton(ContentDialogButton.PRIMARY);
            dialog.addButtonClickListener(e -> {
                if (e.getButton() == ContentDialogButton.PRIMARY && !age.getText().matches("\\d+")) {
                    error.setText("Enter a number");
                    e.setCancel(true);              // keep the dialog open
                }
            });
            if (dialog.showAndWait() == ContentDialogResult.PRIMARY) {
                status.setText("Age: " + age.getText());
            }
        });
    }
}
