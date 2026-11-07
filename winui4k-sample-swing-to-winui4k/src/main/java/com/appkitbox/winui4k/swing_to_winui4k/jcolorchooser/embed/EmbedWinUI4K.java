package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.embed;

import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFont;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 28.3 Put a color chooser on the window and follow its changes (WinUI4K). */
public class EmbedWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("28.3 Embed (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WColorPicker picker = new WColorPicker(WColor.ORANGE);
            WLabel preview = new WLabel("Sample text");
            preview.setFont(new WFont("Segoe UI", WFont.BOLD, 24));
            picker.getSelectionModel().addChangeListener(e -> preview.setForeground(picker.getColor()));

            frame.add(picker);
            frame.add(preview);

            frame.setSize(720, 780);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            picker.setColor(0, 120, 215);
        });
    }
}
