package com.appkitbox.winui4k.swing_to_winui4k.layout.spring;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WRelativePanel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.7 SpringLayout (WinUI4K). */
public class SpringWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.7 SpringLayout (WinUI4K)");

            WRelativePanel panel = new WRelativePanel();
            panel.setMargin(10);                         // the distance from the edges of the window

            WLabel label = new WLabel("Name:");
            WTextField field = new WTextField(15);
            WButton ok = new WButton("OK");
            panel.add(label);
            panel.add(field);
            panel.add(ok);

            // the field to the right of the label (the gap is the margin of the field)
            panel.placeRightOf(field, label);
            field.setMargin(5, 0, 0, 0);                 // left, top, right, bottom
            label.setMargin(0, 6, 0, 0);                 // no baseline: move the label down to the middle of the field
            // the button at the bottom right of the panel
            panel.alignRightWithPanel(ok);
            panel.alignBottomWithPanel(ok);

            frame.setContentPane(panel);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
