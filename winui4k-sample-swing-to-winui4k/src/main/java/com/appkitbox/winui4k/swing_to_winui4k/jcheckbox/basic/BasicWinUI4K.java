package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.basic;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 5.2 Read the check state (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("5.2 Read the check state (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WCheckBox remember = new WCheckBox("Remember me", true);
            WCheckBox newsletter = new WCheckBox("Subscribe to the newsletter");
            WLabel result = new WLabel();
            WButton ok = new WButton("OK");
            ok.addActionListener(e -> result.setText(
                    "remember=" + remember.isSelected() + ", newsletter=" + newsletter.isSelected()));

            frame.add(remember);
            frame.add(newsletter);
            frame.add(ok);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            ok.doClick(); // show the result once at startup
        });
    }
}
