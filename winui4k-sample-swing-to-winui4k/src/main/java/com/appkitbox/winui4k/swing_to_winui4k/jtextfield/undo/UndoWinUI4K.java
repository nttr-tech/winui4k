package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.undo;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.13 Undo and redo (WinUI4K). */
public class UndoWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.13 Undo and Redo (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField memo = new WTextField(30);
            memo.setText("Undoable note");
            // Ctrl+Z / Ctrl+Y work without any code. To call them from a menu or the like:
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
