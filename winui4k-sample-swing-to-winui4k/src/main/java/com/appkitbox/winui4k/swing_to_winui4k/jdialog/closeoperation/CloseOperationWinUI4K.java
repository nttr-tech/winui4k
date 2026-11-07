package com.appkitbox.winui4k.swing_to_winui4k.jdialog.closeoperation;

import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WContentDialogAdapter;
import com.appkitbox.winui4k.WContentDialogEvent;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 25.4 Handle the close operation yourself (WinUI4K). */
public class CloseOperationWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("25.4 Close Operation (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            WLabel message = new WLabel("You have unsaved changes.");
            WContentDialog dialog = new WContentDialog(frame, "Settings", true);
            dialog.setDefaultCloseOperation(WContentDialog.DO_NOTHING_ON_CLOSE);   // Esc does not close
            dialog.addDialogListener(new WContentDialogAdapter() {
                @Override
                public void dialogClosing(WContentDialogEvent e) {
                    if (e.getResult() == ContentDialogResult.NONE) {
                        message.setText("Click Save or Discard to close.");
                    }
                }
            });
            dialog.add(message);
            dialog.setPrimaryButtonText("Save");
            dialog.setSecondaryButtonText("Discard");
            dialog.showAndWait();
        });
    }
}
