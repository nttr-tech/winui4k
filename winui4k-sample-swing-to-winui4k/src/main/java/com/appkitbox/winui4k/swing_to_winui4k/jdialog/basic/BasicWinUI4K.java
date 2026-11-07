package com.appkitbox.winui4k.swing_to_winui4k.jdialog.basic;

import com.appkitbox.winui4k.ContentDialogButton;
import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 25.2 Open an OK / Cancel dialog and read the input (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("25.2 OK and Cancel (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Renaming...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            WTextField name = new WTextField(16);
            name.setText("report.txt");
            WContentDialog dialog = new WContentDialog(frame, "Rename", true);   // always modal
            dialog.add(new WLabel("New name:"), WBorderLayout.NORTH);
            dialog.add(name, WBorderLayout.CENTER);
            dialog.setPrimaryButtonText("OK");
            dialog.setCloseButtonText("Cancel");
            dialog.setDefaultButton(ContentDialogButton.PRIMARY);
            ContentDialogResult result = dialog.showAndWait();   // blocks until the dialog closes
            if (result == ContentDialogResult.PRIMARY) {
                status.setText("Renamed to " + name.getText());
            }
        });
    }
}
