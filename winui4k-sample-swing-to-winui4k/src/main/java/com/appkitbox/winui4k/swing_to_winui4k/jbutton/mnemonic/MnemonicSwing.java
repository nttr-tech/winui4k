package com.appkitbox.winui4k.swing_to_winui4k.jbutton.mnemonic;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 1.6 Press with a keyboard mnemonic (Swing). */
public class MnemonicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.6 Mnemonic (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton save = new JButton("Save");
            save.setMnemonic(KeyEvent.VK_S);

            frame.add(save);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
