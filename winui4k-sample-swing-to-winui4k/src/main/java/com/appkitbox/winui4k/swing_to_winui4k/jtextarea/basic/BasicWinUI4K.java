package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.basic;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 12.2 Create a text area and read and write its text (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.2 Read and write text (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane memo = new WTextPane(5, 30);
            memo.setText("Buy milk\nCall Alice\nSend the report");
            WLabel status = new WLabel();
            WButton count = new WButton("Count lines");
            count.addActionListener(e -> status.setText(memo.getLineCount() + " lines, "
                    + memo.getText().length() + " characters"));

            frame.add(memo);
            frame.add(count);
            frame.add(status);

            frame.setSize(480, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            count.doClick();
        });
    }
}
