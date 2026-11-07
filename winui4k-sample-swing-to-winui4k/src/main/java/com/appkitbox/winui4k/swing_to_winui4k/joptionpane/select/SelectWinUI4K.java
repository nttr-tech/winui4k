package com.appkitbox.winui4k.swing_to_winui4k.joptionpane.select;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WOptionPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 26.5 Choose from a list (WinUI4K). */
public class SelectWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("26.5 Select (WinUI4K)");
            frame.setLayout(new WFlowLayout());
            WLabel status = new WLabel("Waiting for the answer...");
            frame.add(status);
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // a dialog opens only after the window has been laid out once
            WinUiUtilities.schedule(500, () -> {
                String[] sizes = {"Small", "Medium", "Large"};
                Object size = WOptionPane.showInputDialog(frame, "Choose a size:", "Size",
                        WOptionPane.QUESTION_MESSAGE, null, sizes, "Medium");
                if (size != null) {
                    status.setText("Size: " + size);
                }
            });
        });
    }
}
