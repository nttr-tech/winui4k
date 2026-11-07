package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.toggle;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledEditorKit;

/** 13.4 Bold and italic buttons for the selection (Swing). */
public class ToggleSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.4 Bold and italic buttons (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextPane pane = new JTextPane();
            pane.setText("Select some words and press a button.");
            pane.setPreferredSize(new Dimension(400, 100));
            JButton bold = new JButton(new StyledEditorKit.BoldAction());
            bold.setText("Bold");
            JButton italic = new JButton(new StyledEditorKit.ItalicAction());
            italic.setText("Italic");

            frame.add(new JScrollPane(pane));
            frame.add(bold);
            frame.add(italic);

            frame.setSize(560, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Select "some words" and press Bold (the action edits the text pane given as the event source)
            pane.select(7, 17);
            bold.getAction().actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, "Bold"));
        });
    }
}
