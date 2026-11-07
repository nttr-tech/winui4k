package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.nested;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WSplitPane;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 17.5 Nest split panes into three areas (WinUI4K). */
public class NestedWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("17.5 Nested (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WList files = new WList(new String[] {"Main.java", "Util.java"});
            WTextPane editor = new WTextPane();
            editor.setText("class Main {}");
            WTextPane console = new WTextPane();
            console.setText("BUILD SUCCESSFUL");

            WSplitPane right = new WSplitPane(WSplitPane.VERTICAL_SPLIT, editor, console);
            right.setResizeWeight(1.0);         // only the editor grows
            WSplitPane main = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT, files, right);
            main.setDividerLocation(120);
            // no setOneTouchExpandable in WinUI4K

            frame.add(main, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            right.setDividerLocation(150);
        });
    }
}
