package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.html;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WRichTextBlock;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.7 Show read-only styled text (WinUI4K). */
public class HtmlWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.7 Read-only styled text (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WRichTextBlock notice = new WRichTextBlock();
            notice.addParagraph(p -> {
                p.run("Version 2.0 is ");
                p.bold("ready");
                p.run(". Read the ");
                p.italic("release notes");
                p.run(" before you ");
                p.underline("upgrade");
                p.run(".");
            });
            notice.addParagraph(p -> p.run("Old settings are kept."));
            notice.setWidth(400);

            frame.add(notice);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
