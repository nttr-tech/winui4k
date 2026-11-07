package com.appkitbox.winui4k.swing_to_winui4k.jpanel.nest;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 14.2 Nest panels (WinUI4K). */
public class NestWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("14.2 Nest (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLayoutPanel form = new WLayoutPanel(new WFlowLayout(WFlowLayout.LEFT));
            form.add(new WLabel("Name:"));
            form.add(new WTextField(15));

            WLayoutPanel options = new WLayoutPanel(new WGridLayout(0, 1));
            options.add(new WCheckBox("Send me the newsletter"));
            options.add(new WCheckBox("Remember me"));

            WLayoutPanel buttons = new WLayoutPanel(new WFlowLayout(WFlowLayout.RIGHT));
            buttons.add(new WButton("OK"));
            buttons.add(new WButton("Cancel"));

            frame.add(form, WBorderLayout.NORTH);
            frame.add(options, WBorderLayout.CENTER);
            frame.add(buttons, WBorderLayout.SOUTH);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
