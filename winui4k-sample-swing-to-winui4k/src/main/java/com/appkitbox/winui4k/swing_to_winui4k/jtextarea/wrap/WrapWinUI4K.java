package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.wrap;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 12.3 Wrap long lines (WinUI4K). */
public class WrapWinUI4K {
    private static final String TEXT =
            "WinUI4K lets you call WinUI from Java and Kotlin, so a Swing application can be moved to a modern look.";

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.3 Wrap long lines (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane noWrap = new WTextPane(TEXT, 4, 20);      // does not wrap by default

            WTextPane wrapped = new WTextPane(TEXT, 4, 20);
            wrapped.setLineWrap(true);
            wrapped.setWrapStyleWord(true);                     // break at word boundaries

            frame.add(noWrap);
            frame.add(wrapped);

            frame.setSize(560, 340);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
