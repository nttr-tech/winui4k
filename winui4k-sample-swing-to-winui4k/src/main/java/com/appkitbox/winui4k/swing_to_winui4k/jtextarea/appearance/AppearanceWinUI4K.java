package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.appearance;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 12.6 Read-only text with a monospaced font (WinUI4K). */
public class AppearanceWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.6 Read-only and font (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane license = new WTextPane(6, 50);
            license.setText("MIT License\n\nPermission is hereby granted, free of charge,\n"
                    + "to any person obtaining a copy of this software.");
            license.setEditable(false);
            license.setFont(new WFont("Consolas", WFont.PLAIN, 14));
            license.setForeground(new WColor(0, 90, 158));

            frame.add(license);

            frame.setSize(640, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
