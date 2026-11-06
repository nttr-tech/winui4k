package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** 2.12 A key binding that clears the input with Esc (Swing). */
public class KeyBindingSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2.12 Clearing the input with Esc (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField query = new JTextField(20);
            query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
            query.getActionMap().put("clear", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    query.setText("");
                }
            });
            query.setText("Words to search for");

            frame.add(query);

            frame.setSize(400, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
