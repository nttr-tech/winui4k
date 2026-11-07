package com.appkitbox.winui4k.swing_to_winui4k.jbutton.action;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.8 Share button and menu handling with an Action (WinUI4K). */
public class ActionWinUI4K {
    private static void save() {
        System.out.println("Saved");
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.8 Action (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WAction saveAction = new WAbstractAction("Save", Symbol.SAVE) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    save();
                }
            };
            saveAction.putValue(WAction.SHORT_DESCRIPTION, "Save the file");

            WButton saveButton = new WButton(saveAction);
            WMenuFlyoutItem saveItem = new WMenuFlyoutItem(saveAction);

            saveAction.setEnabled(false);   // disables both the button and the menu item

            frame.add(saveButton);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
