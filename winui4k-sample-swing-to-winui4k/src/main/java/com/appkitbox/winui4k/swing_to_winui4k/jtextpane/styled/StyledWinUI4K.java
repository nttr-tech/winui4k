package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.styled;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSimpleAttributeSet;
import com.appkitbox.winui4k.WStyleConstants;
import com.appkitbox.winui4k.WStyledDocument;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.2 Insert text with styles (WinUI4K). */
public class StyledWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.2 Insert styled text (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane pane = new WTextPane();
            pane.setLineWrap(true);  // wraps long lines like JTextPane
            pane.setWidth(400);
            pane.setHeight(120);

            WSimpleAttributeSet plain = new WSimpleAttributeSet();
            WSimpleAttributeSet bold = new WSimpleAttributeSet();
            WStyleConstants.setBold(bold, true);
            WSimpleAttributeSet red = new WSimpleAttributeSet();
            WStyleConstants.setForeground(red, WColor.RED);
            WStyleConstants.setItalic(red, true);
            WSimpleAttributeSet big = new WSimpleAttributeSet();
            WStyleConstants.setFontSize(big, 20);
            WStyleConstants.setUnderline(big, true);

            WStyledDocument doc = pane.getStyledDocument();
            doc.insertString(doc.getLength(), "Plain, ", plain);
            doc.insertString(doc.getLength(), "bold, ", bold);
            doc.insertString(doc.getLength(), "red italic, ", red);
            doc.insertString(doc.getLength(), "big underline", big);

            frame.add(pane);

            frame.setSize(480, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
