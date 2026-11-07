package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.styled;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** 13.2 Insert text with styles (Swing). */
public class StyledSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.2 Insert styled text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextPane pane = new JTextPane();
            pane.setPreferredSize(new Dimension(400, 120));

            SimpleAttributeSet plain = new SimpleAttributeSet();
            SimpleAttributeSet bold = new SimpleAttributeSet();
            StyleConstants.setBold(bold, true);
            SimpleAttributeSet red = new SimpleAttributeSet();
            StyleConstants.setForeground(red, Color.RED);
            StyleConstants.setItalic(red, true);
            SimpleAttributeSet big = new SimpleAttributeSet();
            StyleConstants.setFontSize(big, 20);
            StyleConstants.setUnderline(big, true);

            StyledDocument doc = pane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), "Plain, ", plain);
                doc.insertString(doc.getLength(), "bold, ", bold);
                doc.insertString(doc.getLength(), "red italic, ", red);
                doc.insertString(doc.getLength(), "big underline", big);
            } catch (BadLocationException e) {
                throw new IllegalStateException(e);
            }

            frame.add(new JScrollPane(pane));

            frame.setSize(480, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
