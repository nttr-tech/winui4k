package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.selected;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/** 6.3 Read the selected button (Swing). */
public class SelectedSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("6.3 Read the selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JRadioButton card = new JRadioButton("Credit card", true);
            JRadioButton bank = new JRadioButton("Bank transfer");
            card.setActionCommand("card");
            bank.setActionCommand("bank");
            ButtonGroup payment = new ButtonGroup();
            payment.add(card);
            payment.add(bank);

            JLabel result = new JLabel();
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> result.setText("Payment: " + payment.getSelection().getActionCommand()));

            frame.add(card);
            frame.add(bank);
            frame.add(ok);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            bank.setSelected(true); // choose "Bank transfer" and press OK once at startup
            ok.doClick();
        });
    }
}
