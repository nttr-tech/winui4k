package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.clear;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WButtonGroup;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 6.5 Select from code and clear the selection (WinUI4K). */
public class ClearWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("6.5 Select and clear (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WRadioButton yes = new WRadioButton("Yes");
            WRadioButton no = new WRadioButton("No");
            WButtonGroup answer = new WButtonGroup();
            answer.add(yes);
            answer.add(no);
            answer.setSelected(no, true);                          // same as no.setSelected(true)

            WButton reset = new WButton("Reset");
            reset.addActionListener(e -> answer.clearSelection()); // no button is selected

            frame.add(yes);
            frame.add(no);
            frame.add(reset);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
