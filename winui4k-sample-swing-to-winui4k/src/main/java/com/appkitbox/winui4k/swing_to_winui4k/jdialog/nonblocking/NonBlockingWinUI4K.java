package com.appkitbox.winui4k.swing_to_winui4k.jdialog.nonblocking;

import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 25.5 Open a dialog without blocking (WinUI4K). */
public class NonBlockingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("25.5 Without Blocking (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Opening the Find dialog...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            WTextField text = new WTextField(12);
            text.setText("needle");
            WContentDialog dialog = new WContentDialog(frame, "Find", true);   // modeless is not available
            dialog.add(text);
            dialog.setPrimaryButtonText("Find");
            dialog.setCloseButtonText("Close");
            dialog.show(status, result -> {           // returns immediately
                if (result == ContentDialogResult.PRIMARY) {
                    status.setText("Searching for " + text.getText());
                }
            });
            status.setText("The Find dialog is open");
        });
    }
}
