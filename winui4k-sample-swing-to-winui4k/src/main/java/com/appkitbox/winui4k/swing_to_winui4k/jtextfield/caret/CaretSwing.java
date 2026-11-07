package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.caret;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 2.11 Work with the selection and caret (Swing). */
public class CaretSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.11 Selection and Caret (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel position = new JLabel("Position: 0");
            JTextField text = new JTextField(30);
            text.setText("Hello, World");
            text.select(7, 12);                          // select "World"
            String selected = text.getSelectedText();    // "World"
            System.out.println("Selected text: " + selected);
            text.replaceSelection("Swing");              // "Hello, Swing"
            text.setCaretPosition(text.getText().length());   // move to the end
            text.addCaretListener(e -> position.setText("Position: " + e.getDot()));
            position.setText("Position: " + text.getCaretPosition());
            text.select(7, 12);                          // leave the replaced word selected

            frame.add(text);
            frame.add(position);

            frame.setSize(560, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
