package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.caret;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/** 13.5 Read the style at the caret (Swing). */
public class CaretSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.5 Style at the caret (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextPane pane = new JTextPane();
            pane.setText("Normal text, bold text, italic text.");
            SimpleAttributeSet bold = new SimpleAttributeSet();
            StyleConstants.setBold(bold, true);
            pane.getStyledDocument().setCharacterAttributes(13, 9, bold, false);
            SimpleAttributeSet italic = new SimpleAttributeSet();
            StyleConstants.setItalic(italic, true);
            pane.getStyledDocument().setCharacterAttributes(24, 11, italic, false);
            pane.setPreferredSize(new Dimension(400, 80));

            JLabel state = new JLabel("Plain");
            pane.addCaretListener(e -> {
                AttributeSet attrs = pane.getCharacterAttributes();
                state.setText((StyleConstants.isBold(attrs) ? "Bold" : "Plain")
                        + ", size " + StyleConstants.getFontSize(attrs));
            });

            frame.add(new JScrollPane(pane));
            frame.add(state);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            pane.setCaretPosition(16);   // inside "bold"
            pane.requestFocusInWindow();
        });
    }
}
