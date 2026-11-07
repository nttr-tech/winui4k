package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.show;

import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/** 29.4 Show a tooltip from the program (Swing). */
public class ShowSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // for the screenshot: Metal shows tooltips only while the window is active
            UIManager.put("ToolTipManager.enableToolTipMode", "allWindows");
            JFrame frame = new JFrame("29.4 Show (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextField zip = new JTextField("12-345", 10);
            zip.setToolTipText("Enter 7 digits, like 123-4567");

            frame.add(zip);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // e.g. after a failed check: Swing has no API for this, so fake the mouse moving over the field
            Timer timer = new Timer(500, e -> {
                ToolTipManager manager = ToolTipManager.sharedInstance();
                manager.setInitialDelay(0);
                MouseEvent move = new MouseEvent(zip, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, 0, 0, 0, false);
                manager.mouseEntered(move);
                manager.mouseMoved(move);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
