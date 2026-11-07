package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.message;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 26.2 Show a message (WinUI4K). */
public class MessageWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("26.2 Message (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // a dialog opens only after the window has been laid out once
            WinUiUtilities.schedule(500, () -> {
                WOptionPane.showMessageDialog(frame, "The file could not be saved.",
                        "Save Failed", WOptionPane.ERROR_MESSAGE);
                status.setText("The message was closed");
            });
        });
    }
}
