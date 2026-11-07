package com.appkitbox.winui4k.swing_to_winui4k.jframe.state;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 24.6 Maximize, minimize, and fix the size (WinUI4K). */
public class StateWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("24.6 Window State (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel state = new WLabel("State: normal");
            WButton maximize = new WButton("Maximize");
            maximize.addActionListener(e -> frame.setExtendedState(WFrame.MAXIMIZED_BOTH));
            WButton minimize = new WButton("Minimize");
            minimize.addActionListener(e -> frame.setExtendedState(WFrame.ICONIFIED));
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
