package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.action;

import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 7.5 Share the state with an Action (WinUI4K). */
public class ActionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("7.5 Share the state (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("Word wrap: false");
            WAction wordWrap = new WAbstractAction("Word wrap") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    status.setText("Word wrap: " + getValue(SELECTED_KEY));
                }
            };
            wordWrap.putValue(WAction.SELECTED_KEY, false);
            WToggleButton toolBarButton = new WToggleButton(wordWrap);
            WCheckBox settingsCheckBox = new WCheckBox(wordWrap);   // both follow SELECTED_KEY

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
