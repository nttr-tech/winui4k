package com.appkitbox.winui4k.swing_to_winui4k.jframe.state;

import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 24.6 Maximize, minimize, and fix the size (Swing). */
public class StateSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("24.6 Window State (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel state = new JLabel("State: normal");
            JButton maximize = new JButton("Maximize");
            maximize.addActionListener(e -> frame.setExtendedState(Frame.MAXIMIZED_BOTH));
            JButton minimize = new JButton("Minimize");
            minimize.addActionListener(e -> frame.setExtendedState(Frame.ICONIFIED));
            frame.addWindowStateListener(e -> state.setText("State: " + e.getOldState() + " -> " + e.getNewState()));
            frame.setResizable(false);   // the user cannot resize or maximize the window

            frame.add(maximize);
            frame.add(minimize);
            frame.add(state);

            frame.setSize(420, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
