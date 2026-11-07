package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.highlight;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSimpleAttributeSet;
import com.appkitbox.winui4k.WStyleConstants;
import com.appkitbox.winui4k.WStyledDocument;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.3 Change the style of a range (WinUI4K). */
public class HighlightWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.3 Style a range (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane pane = new WTextPane();
            pane.setLineWrap(true);  // wraps long lines like JTextPane
            pane.setText("INFO start\nERROR disk full\nINFO retry\nERROR network down");
            pane.setWidth(400);
            pane.setHeight(120);
            WTextField word = new WTextField(10);
            word.setText("ERROR");
            WButton mark = new WButton("Mark");
            mark.addActionListener(e -> {
                WSimpleAttributeSet red = new WSimpleAttributeSet();
                WStyleConstants.setForeground(red, WColor.RED);
                WStyleConstants.setBold(red, true);
                WStyledDocument doc = pane.getStyledDocument();
                String text = pane.getText();
                String target = word.getText();
                for (int i = text.indexOf(target); i >= 0 && !target.isEmpty(); i = text.indexOf(target, i + 1)) {
                    doc.setCharacterAttributes(i, target.length(), red, false);
                }
            });

            frame.add(pane);
            frame.add(word);
            frame.add(mark);

            frame.setSize(480, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            mark.doClick();
        });
    }
}
