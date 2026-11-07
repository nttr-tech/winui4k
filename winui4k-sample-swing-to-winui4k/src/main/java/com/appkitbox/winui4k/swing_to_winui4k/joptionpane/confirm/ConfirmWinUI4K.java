package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.confirm;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 26.3 Ask yes or no (WinUI4K). */
public class ConfirmWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("26.3 Confirm (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            int answer = WOptionPane.showConfirmDialog(frame, "Delete the selected file?",
                    "Delete", WOptionPane.YES_NO_OPTION);
            if (answer == WOptionPane.YES_OPTION) {
                status.setText("Deleted");
            }
        });
    }
}
