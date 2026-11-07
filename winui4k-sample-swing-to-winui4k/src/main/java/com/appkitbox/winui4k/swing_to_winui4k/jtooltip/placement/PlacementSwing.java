package com.appkitbox.winui4k.swing_to_winui4k.jtooltip.placement;

import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/** 29.2 Choose where the tooltip appears (Swing). */
public class PlacementSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // for the screenshot: Metal shows tooltips only while the window is active
            UIManager.put("ToolTipManager.enableToolTipMode", "allWindows");
            JFrame frame = new JFrame("29.2 Placement (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton save = new JButton("Save") {
                @Override
                public Point getToolTipLocation(MouseEvent event) {
                    return new Point(0, getHeight());   // just below the button
                }
            };
            save.setToolTipText("Save the file (Ctrl+S)");

            frame.add(save);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // for the screenshot: show the tooltip without the mouse (see 29.4)
            Timer timer = new Timer(500, e -> {
                ToolTipManager manager = ToolTipManager.sharedInstance();
                manager.setInitialDelay(0);
                MouseEvent move = new MouseEvent(save, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, 0, 0, 0, false);
                manager.mouseEntered(move);
                manager.mouseMoved(move);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
