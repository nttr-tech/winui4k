package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.8 Sharing the handling of a button and a menu with an Action (WinUI4K). */
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
            saveAction.putValue(WAction.SHORT_DESCRIPTION, "Saves the file");

            WButton saveButton = new WButton(saveAction);
            WMenuFlyoutItem saveItem = new WMenuFlyoutItem(saveAction);

            saveAction.setEnabled(false);   // Both the button and the menu become disabled

            frame.add(saveButton);

            frame.setSize(400, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
