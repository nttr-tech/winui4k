package com.appkitbox.winui4k.swing_to_winui4k.jradiobutton.clear;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/** 6.5 Select from code and clear the selection (Swing). */
public class ClearSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("6.5 Select and clear (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JRadioButton yes = new JRadioButton("Yes");
            JRadioButton no = new JRadioButton("No");
            ButtonGroup answer = new ButtonGroup();
            answer.add(yes);
            answer.add(no);
            answer.setSelected(no.getModel(), true);              // same as no.setSelected(true)

            JButton reset = new JButton("Reset");
            reset.addActionListener(e -> answer.clearSelection()); // no button is selected

            frame.add(yes);
            frame.add(no);
            frame.add(reset);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
