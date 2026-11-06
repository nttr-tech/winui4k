package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

/** 2.13 Undo and redo (Swing). */
public class UndoSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.13 Undo and redo (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField memo = new JTextField(30);
            UndoManager undo = new UndoManager();
            memo.getDocument().addUndoableEditListener(undo);
            memo.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
            memo.getActionMap().put("undo", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (undo.canUndo()) undo.undo();
                }
            });
            // Redo (Ctrl+Y) is written the same way
            memo.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
            memo.getActionMap().put("redo", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (undo.canRedo()) undo.redo();
                }
            });
            memo.setText("A memo that can be undone");

            // To look the same as the WinUI4K side, make them callable from buttons too
            JButton undoButton = new JButton("Undo");
            JButton redoButton = new JButton("Redo");
            undoButton.addActionListener(e -> { if (undo.canUndo()) undo.undo(); });
            redoButton.addActionListener(e -> { if (undo.canRedo()) undo.redo(); });

            frame.add(memo);
            frame.add(undoButton);
            frame.add(redoButton);

            frame.setSize(560, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
