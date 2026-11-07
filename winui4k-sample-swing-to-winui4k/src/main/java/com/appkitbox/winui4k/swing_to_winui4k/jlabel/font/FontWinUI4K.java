package com.appkitbox.winui4k.swing_to_winui4k.jlabel.font;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.3 Font and color (WinUI4K). */
public class FontWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.3 Font and color (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel heading = new WLabel("Monthly report");
            heading.setFont(heading.getFont().deriveFont(WFont.BOLD, 20f));
            WLabel error = new WLabel("The file could not be saved.");
            error.setForeground(WColor.RED);
            WLabel note = new WLabel("Last updated: 2026-11-07");
            note.setFont(new WFont("Segoe UI", WFont.ITALIC, 12));

            frame.add(heading);
            frame.add(error);
            frame.add(note);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
