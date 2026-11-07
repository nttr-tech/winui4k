package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.lines;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/** 12.5 Show the line and column of the caret (Swing). */
public class LinesSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("12.5 Line and column (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextArea editor = new JTextArea("first line\nsecond line\nthird line", 5, 30);
            JLabel position = new JLabel("Ln 1, Col 1");
            editor.addCaretListener(e -> {
                try {
                    int line = editor.getLineOfOffset(e.getDot());
                    int column = e.getDot() - editor.getLineStartOffset(line);
                    position.setText("Ln " + (line + 1) + ", Col " + (column + 1));
                } catch (BadLocationException ex) {
                    throw new IllegalStateException(ex);
                }
            });

            frame.add(new JScrollPane(editor));
            frame.add(position);

            frame.setSize(480, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            editor.setCaretPosition(editor.getText().indexOf("line", 11));   // "line" of the second line
            editor.requestFocusInWindow();
        });
    }
}
