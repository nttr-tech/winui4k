package com.appkitbox.winui4k.swing_to_winui4k.jlabel.labelfor;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.6 Label for an input field (WinUI4K). */
public class LabelForWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.6 Label for a field (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel nameLabel = new WLabel("Name:");
            WTextField name = new WTextField(15);
            nameLabel.setLabelFor(name);
            nameLabel.setDisplayedMnemonic('N');                   // Alt+N moves the focus to the field

            WLabel emailLabel = new WLabel("Email:");
            WTextField email = new WTextField(15);
            emailLabel.setLabelFor(email);
            emailLabel.setDisplayedMnemonic('E');

            frame.add(nameLabel);
            frame.add(name);
            frame.add(emailLabel);
            frame.add(email);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
