package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.14 Accent and flyout (WinUI4K). */
public class AccentFlyoutWinUI4K {
    private static void exportPdf() {
        System.out.println("Exported as PDF");
    }

    private static void exportCsv() {
        System.out.println("Exported as CSV");
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.14 Accent and flyout (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            // Emphasize the most important operation on the screen with the accent color from the Windows settings
            WButton submit = new WButton("Submit");
            submit.setAccent(true);

            // A button that opens a menu when pressed (in Swing you show a JPopupMenu yourself)
            WMenuFlyout menu = new WMenuFlyout();
            menu.add("Export as PDF").addActionListener(() -> exportPdf());
            menu.add("Export as CSV").addActionListener(() -> exportCsv());
            WButton export = new WButton("Export");
            export.setFlyout(menu);

            frame.add(submit);
            frame.add(export);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
