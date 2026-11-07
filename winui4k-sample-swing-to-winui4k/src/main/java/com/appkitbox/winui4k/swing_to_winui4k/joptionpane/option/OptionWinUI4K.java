package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.option;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 26.6 Use your own button labels (WinUI4K). */
public class OptionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("26.6 Option Buttons (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            String[] options = {"Save", "Don't Save", "Cancel"};
            int choice = WOptionPane.showOptionDialog(frame, "Save changes to report.txt?", "Notepad",
                    WOptionPane.YES_NO_CANCEL_OPTION, WOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                status.setText("Saved");
            }
        });
    }
}
