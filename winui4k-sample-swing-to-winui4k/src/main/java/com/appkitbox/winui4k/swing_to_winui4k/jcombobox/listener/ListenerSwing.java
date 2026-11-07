package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.listener;

import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 8.3 React to selection changes (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("8.3 Selection changes (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JComboBox<String> color = new JComboBox<>(new String[] {"Red", "Green", "Blue"});
            JLabel log = new JLabel("No change yet");
            color.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    log.setText("Selected: " + e.getItem());
                }
            });

            frame.add(color);
            frame.add(log);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            color.setSelectedItem("Blue");
        });
    }
}
