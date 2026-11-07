package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.caret;

import com.appkitbox.winui4k.WAttributeSet;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSimpleAttributeSet;
import com.appkitbox.winui4k.WStyleConstants;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.5 Read the style at the caret (WinUI4K). */
public class CaretWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.5 Style at the caret (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane pane = new WTextPane();
            pane.setText("Normal text, bold text, italic text.");
            WSimpleAttributeSet bold = new WSimpleAttributeSet();
            WStyleConstants.setBold(bold, true);
            pane.getStyledDocument().setCharacterAttributes(13, 9, bold, false);
            WSimpleAttributeSet italic = new WSimpleAttributeSet();
            WStyleConstants.setItalic(italic, true);
            pane.getStyledDocument().setCharacterAttributes(24, 11, italic, false);
            pane.setWidth(400);
            pane.setHeight(80);

            WLabel state = new WLabel("Plain");
            pane.addCaretListener(e -> {
                WAttributeSet attrs = pane.getCharacterAttributes();
                state.setText((WStyleConstants.isBold(attrs) ? "Bold" : "Plain")
                        + ", size " + WStyleConstants.getFontSize(attrs));
            });

            frame.add(pane);
            frame.add(state);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            pane.setCaretPosition(16);   // inside "bold"
            pane.requestFocusInWindow();
        });
    }
}
