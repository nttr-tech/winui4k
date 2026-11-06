package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.10 Clicking from the program (WinUI4K). */
public class DoClickWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.10 doClick (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel message = new WLabel("Not searched yet");
            WButton search = new WButton("Search");
            search.addActionListener(e -> message.setText("Searched"));

            frame.add(search);
            frame.add(message);

            search.doClick();

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
