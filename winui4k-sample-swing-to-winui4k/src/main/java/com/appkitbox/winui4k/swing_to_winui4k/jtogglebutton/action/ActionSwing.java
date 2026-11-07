package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.action;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** 7.5 Share the state with an Action (Swing). */
public class ActionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("7.5 Share the state (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("Word wrap: false");
            Action wordWrap = new AbstractAction("Word wrap") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    status.setText("Word wrap: " + getValue(SELECTED_KEY));
                }
            };
            wordWrap.putValue(Action.SELECTED_KEY, false);
            JToggleButton toolBarButton = new JToggleButton(wordWrap);
            JCheckBox settingsCheckBox = new JCheckBox(wordWrap);   // both follow SELECTED_KEY

            frame.add(toolBarButton);
            frame.add(settingsCheckBox);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            settingsCheckBox.doClick(); // the toggle button is selected too
        });
    }
}
