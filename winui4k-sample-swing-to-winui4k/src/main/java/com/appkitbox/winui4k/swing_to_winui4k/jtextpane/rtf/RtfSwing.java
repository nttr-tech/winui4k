package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.rtf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.rtf.RTFEditorKit;

/** 13.6 Save and restore styled text as RTF (Swing). */
public class RtfSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.6 Save and restore as RTF (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextPane source = new JTextPane();
            source.setText("Saved with bold and blue text.");
            SimpleAttributeSet bold = new SimpleAttributeSet();
            StyleConstants.setBold(bold, true);
            source.getStyledDocument().setCharacterAttributes(11, 4, bold, false);
            SimpleAttributeSet blue = new SimpleAttributeSet();
            StyleConstants.setForeground(blue, Color.BLUE);
            source.getStyledDocument().setCharacterAttributes(20, 4, blue, false);
            source.setPreferredSize(new Dimension(400, 60));

            JTextPane copy = new JTextPane();
            copy.setPreferredSize(new Dimension(400, 60));

            JButton transfer = new JButton("Copy as RTF");
            transfer.addActionListener(e -> {
                try {
                    RTFEditorKit kit = new RTFEditorKit();
                    ByteArrayOutputStream rtf = new ByteArrayOutputStream();
                    kit.write(rtf, source.getDocument(), 0, source.getDocument().getLength());

                    copy.setEditorKit(kit);
                    Document restored = kit.createDefaultDocument();
                    kit.read(new ByteArrayInputStream(rtf.toByteArray()), restored, 0);
                    copy.setDocument(restored);
                } catch (IOException | BadLocationException ex) {
                    throw new IllegalStateException(ex);
                }
            });

            frame.add(new JScrollPane(source));
            frame.add(transfer);
            frame.add(new JScrollPane(copy));

            frame.setSize(480, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            transfer.doClick();
        });
    }
}
