package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.header;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 12.7 Header and placeholder text (WinUI4K). */
public class HeaderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.7 Header and placeholder (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane comment = new WTextPane();
            comment.setPlaceholderText("Write your feedback here");   // shown while the pane is empty
            comment.setHeader("Feedback");
            comment.setRows(4);
            comment.setColumns(30);

            frame.add(comment);

            frame.setSize(480, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
