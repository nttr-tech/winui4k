package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.13 Undo and redo (WinUI4K). */
public class UndoWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.13 Undo and redo (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField memo = new WTextField(30);
            memo.setText("A memo that can be undone");
            // Ctrl+Z / Ctrl+Y work without writing anything. To call them from a menu etc.
            WButton undoButton = new WButton("Undo");
            WButton redoButton = new WButton("Redo");
            undoButton.addActionListener(e -> { if (memo.canUndo()) memo.undo(); });
            redoButton.addActionListener(e -> { if (memo.canRedo()) memo.redo(); });

            frame.add(memo);
            frame.add(undoButton);
            frame.add(redoButton);

            frame.setSize(560, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
