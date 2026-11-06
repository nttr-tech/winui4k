package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** 2.4 Responding to every input (Swing). */
public class ChangeSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.4 Responding to every input (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField title = new JTextField(30);
            JLabel count = new JLabel("0 characters");
            JButton register = new JButton("Register");
            register.setEnabled(false);

            title.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { update(); }
                @Override public void removeUpdate(DocumentEvent e) { update(); }
                @Override public void changedUpdate(DocumentEvent e) { update(); }

                private void update() {
                    count.setText(title.getText().length() + " characters");
                    register.setEnabled(!title.getText().isEmpty());
                }
            });

            title.setText("Today's memo");

            frame.add(title);
            frame.add(count);
            frame.add(register);

            frame.setSize(560, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
