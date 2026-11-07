package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.position;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WPoint;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 15.4 Scroll from the program (WinUI4K). */
public class PositionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("15.4 Position (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLayoutPanel items = new WLayoutPanel(new WGridLayout(0, 1));
            for (int i = 1; i <= 50; i++) {
                items.add(new WLabel("Item " + i));
            }
            WScrollPane scroll = new WScrollPane(items);

            WButton top = new WButton("Top");
            top.addActionListener(e -> scroll.getViewport().setViewPosition(new WPoint(0, 0)));
            WButton down = new WButton("Down 200");
            down.addActionListener(e -> {
                WPoint p = scroll.getViewport().getViewPosition();
                scroll.getViewport().setViewPosition(new WPoint(p.getX(), p.getY() + 200));
            });
            WLayoutPanel buttons = new WLayoutPanel(new WFlowLayout(WFlowLayout.LEFT));
            buttons.add(top);
            buttons.add(down);

            frame.add(buttons, WBorderLayout.NORTH);
            frame.add(scroll, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            down.doClick();     // scroll down once
        });
    }
}
