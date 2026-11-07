package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.input;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 26.4 Ask for a text (WinUI4K). */
public class InputWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("26.4 Input (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // a dialog opens only after the window has been laid out once
            WinUiUtilities.schedule(500, () -> {
                String name = WOptionPane.showInputDialog(frame, "Your name:", "Alice");
                if (name != null) {                        // null when canceled
                    status.setText("Hello, " + name);
                }
            });
        });
    }
}
