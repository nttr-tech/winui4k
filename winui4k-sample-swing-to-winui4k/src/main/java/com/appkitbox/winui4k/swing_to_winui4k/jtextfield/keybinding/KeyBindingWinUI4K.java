package com.appkitbox.winui4k.swing_to_winui4k.jtextfield.keybinding;

import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WActionEvent;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WKeyEvent;
import com.appkitbox.winui4k.WKeyStroke;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.12 Clear the input with an Esc key binding (WinUI4K). */
public class KeyBindingWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.12 Esc Key Binding (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField query = new WTextField(20);
            query.getInputMap().put(WKeyStroke.getKeyStroke(WKeyEvent.VK_ESCAPE, 0), "clear");
            query.getActionMap().put("clear", new WAbstractAction() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    query.setText("");
                }
            });
            query.setText("search term");

            frame.add(query);

            frame.setSize(400, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
