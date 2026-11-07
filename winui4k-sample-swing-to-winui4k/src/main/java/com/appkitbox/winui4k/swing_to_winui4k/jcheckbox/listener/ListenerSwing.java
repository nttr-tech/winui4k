package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.listener;

import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 5.3 React to check changes (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("5.3 React to changes (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JCheckBox wifi = new JCheckBox("Wi-Fi");
            JLabel state = new JLabel("Wi-Fi is off");
            wifi.addItemListener(e -> state.setText(
                    e.getStateChange() == ItemEvent.SELECTED ? "Wi-Fi is on" : "Wi-Fi is off"));

            frame.add(wifi);
            frame.add(state);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            wifi.setSelected(true); // the listener is also called for programmatic changes
        });
    }
}
