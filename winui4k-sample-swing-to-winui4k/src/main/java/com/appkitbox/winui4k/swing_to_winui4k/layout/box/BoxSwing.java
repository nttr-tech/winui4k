package com.appkitbox.winui4k.swing_to_winui4k.layout.box;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 30.5 BoxLayout and Box (Swing). */
public class BoxSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("30.5 BoxLayout and Box (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // a row of buttons pushed to the right by glue
            Box buttons = Box.createHorizontalBox();
            buttons.add(Box.createHorizontalGlue());
            buttons.add(new JButton("OK"));
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(new JButton("Cancel"));
            buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

            // a column: a fixed gap (strut) between the first two, the rest of the height (glue) above the row
            Box column = Box.createVerticalBox();
            column.add(new JButton("First"));
            column.add(Box.createVerticalStrut(8));
            column.add(new JButton("Second"));
            column.add(Box.createVerticalGlue());
            column.add(buttons);

            frame.add(column);

            frame.setSize(360, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
