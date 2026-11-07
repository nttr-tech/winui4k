package com.appkitbox.winui4k.swing_to_winui4k.layout.absolute;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.8 Absolute positioning with a null layout (WinUI4K). */
public class AbsoluteWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.8 Absolute positioning (WinUI4K)");

            frame.setLayout(null);                       // no layout manager: place each child yourself
            WButton start = new WButton("Start");
            start.setBounds(20, 20, 120, 30);            // x, y, width, height (DIP)
            WLabel note = new WLabel("At (160, 25)");
            note.setBounds(160, 25, 150, 20);
            WButton stop = new WButton("Stop");
            stop.setBounds(60, 80, 200, 50);

            frame.add(start);
            frame.add(note);
            frame.add(stop);

            frame.setSize(360, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
