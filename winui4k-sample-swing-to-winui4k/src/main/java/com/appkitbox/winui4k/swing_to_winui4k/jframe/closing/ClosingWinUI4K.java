package com.appkitbox.winui4k.swing_to_winui4k.jframe.closing;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WWindowAdapter;
import com.appkitbox.winui4k.WWindowEvent;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.3 Confirm before the window closes (WinUI4K). */
public class ClosingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.3 Confirm Closing (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("Close the window with the X button");
            frame.setDefaultCloseOperation(WFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WWindowAdapter() {
                @Override
                public void windowOpened(WWindowEvent e) {
                    status.setText("Opened. Close the window with the X button");
                }

                @Override
                public void windowClosing(WWindowEvent e) {
                    int answer = WOptionPane.showConfirmDialog(frame, "Discard unsaved changes and exit?",
                            "Exit", WOptionPane.YES_NO_OPTION);
                    if (answer == WOptionPane.YES_OPTION) {
                        frame.dispose();
                    }
                }
            });
            frame.add(status);

            frame.setSize(480, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
