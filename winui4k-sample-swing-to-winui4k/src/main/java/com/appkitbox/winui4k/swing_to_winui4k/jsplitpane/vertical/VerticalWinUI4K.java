package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.vertical;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WSplitPane;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 17.3 Split top and bottom by a ratio (WinUI4K). */
public class VerticalWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("17.3 Vertical (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTextPane editor = new WTextPane();         // scrolls by itself
            editor.setText("Editor");
            WTextPane console = new WTextPane();
            console.setText("Console");
            WSplitPane split = new WSplitPane(WSplitPane.VERTICAL_SPLIT, editor, console);
            split.setResizeWeight(0.7);     // the editor gets 70% of extra space
            split.setDividerLocation(0.7);  // can be called before the window is shown

            frame.add(split, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
