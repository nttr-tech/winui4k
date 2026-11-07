package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.rtf;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSimpleAttributeSet;
import com.appkitbox.winui4k.WStyleConstants;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.6 Save and restore styled text as RTF (WinUI4K). */
public class RtfWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.6 Save and restore as RTF (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane source = new WTextPane();
            source.setLineWrap(true);  // wraps long lines like JTextPane
            source.setText("Saved with bold and blue text.");
            WSimpleAttributeSet bold = new WSimpleAttributeSet();
            WStyleConstants.setBold(bold, true);
            source.getStyledDocument().setCharacterAttributes(11, 4, bold, false);
            WSimpleAttributeSet blue = new WSimpleAttributeSet();
            WStyleConstants.setForeground(blue, WColor.BLUE);
            source.getStyledDocument().setCharacterAttributes(20, 4, blue, false);
            source.setWidth(400);
            source.setHeight(60);

            WTextPane copy = new WTextPane();
            copy.setLineWrap(true);  // wraps long lines like JTextPane
            copy.setWidth(400);
            copy.setHeight(60);

            WButton transfer = new WButton("Copy as RTF");
            transfer.addActionListener(e -> {
                String rtf = source.getRtfText();
                copy.setRtfText(rtf);
            });

            frame.add(source);
            frame.add(transfer);
            frame.add(copy);

            frame.setSize(480, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            transfer.doClick();
        });
    }
}
