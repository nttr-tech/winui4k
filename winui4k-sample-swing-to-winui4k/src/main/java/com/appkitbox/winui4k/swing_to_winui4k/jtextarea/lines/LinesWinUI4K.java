package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.lines;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 12.5 Show the line and column of the caret (WinUI4K). */
public class LinesWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.5 Line and column (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane editor = new WTextPane("first line\nsecond line\nthird line", 5, 30);
            WLabel position = new WLabel("Ln 1, Col 1");
            editor.addCaretListener(e -> {
                int line = editor.getLineOfOffset(e.getDot());
                int column = e.getDot() - editor.getLineStartOffset(line);
                position.setText("Ln " + (line + 1) + ", Col " + (column + 1));
            });

            frame.add(editor);
            frame.add(position);

            frame.setSize(480, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            editor.setCaretPosition(editor.getText().indexOf("line", 11));   // "line" of the second line
            editor.requestFocusInWindow();
        });
    }
}
