package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.content;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WToolTip;
import com.appkitbox.winui4k.WinUiUtilities;

/** 29.3 Put more than one line of text in a tooltip (WinUI4K). */
public class ContentWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("29.3 Content (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WButton save = new WButton("Save");
            WLabel title = new WLabel("Save");
            title.setFont(new WFont("Segoe UI", WFont.BOLD, 14));
            WLabel shortcut = new WLabel("Ctrl+S");
            shortcut.setFont(new WFont("Segoe UI", WFont.ITALIC, 12));
            WPanel body = new WPanel(4.0);   // vertical, 4 DIP between the lines
            body.add(title);
            body.add(new WLabel("Save the file to disk."));
            body.add(shortcut);
            WToolTip tip = new WToolTip();
            tip.setContent(body);
            save.setToolTip(tip);

            frame.add(save);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // for the screenshot: show the tooltip without the mouse (see 29.4)
            tip.setOpen(true);
        });
    }
}
