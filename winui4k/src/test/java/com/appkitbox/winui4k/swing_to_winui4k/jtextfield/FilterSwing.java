package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** 2.5 Restricting the characters that can be entered (Swing). */
public class FilterSwing {
    static boolean isDigits(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.5 Restricting characters (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField zip = new JTextField(8);
            ((AbstractDocument) zip.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (isDigits(string)) super.insertString(fb, offset, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text == null || isDigits(text)) super.replace(fb, offset, length, text, attrs);
                }
            });
            zip.setText("1234567");

            frame.add(zip);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
