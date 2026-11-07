package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.maxlength;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** 2.6 Limit the length (Swing). */
public class MaxLengthSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.6 Max Length (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField code = new JTextField(12);
            // Swing has no maximum-length setting, so write a DocumentFilter
            ((AbstractDocument) code.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    int newLength = fb.getDocument().getLength() - length + (text == null ? 0 : text.length());
                    if (newLength <= 10) super.replace(fb, offset, length, text, attrs);
                }

                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    int newLength = fb.getDocument().getLength() + (string == null ? 0 : string.length());
                    if (newLength <= 10) super.insertString(fb, offset, string, attr);
                }
            });
            code.setText("0123456789");

            frame.add(code);

            frame.setSize(420, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
