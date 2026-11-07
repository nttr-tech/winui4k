package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.highlight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** 13.3 Change the style of a range (Swing). */
public class HighlightSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.3 Style a range (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextPane pane = new JTextPane();
            pane.setText("INFO start\nERROR disk full\nINFO retry\nERROR network down");
            pane.setPreferredSize(new Dimension(400, 120));
            JTextField word = new JTextField("ERROR", 10);
            JButton mark = new JButton("Mark");
            mark.addActionListener(e -> {
                SimpleAttributeSet red = new SimpleAttributeSet();
                StyleConstants.setForeground(red, Color.RED);
                StyleConstants.setBold(red, true);
                StyledDocument doc = pane.getStyledDocument();
                String text = pane.getText();
                String target = word.getText();
                for (int i = text.indexOf(target); i >= 0 && !target.isEmpty(); i = text.indexOf(target, i + 1)) {
                    doc.setCharacterAttributes(i, target.length(), red, false);
                }
            });

            frame.add(new JScrollPane(pane));
            frame.add(word);
            frame.add(mark);

            frame.setSize(480, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            mark.doClick();
        });
    }
}
