package com.appkitbox.winui4k.swing_to_winui4k.jtogglebutton.icon;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WinUiUtilities;

/** 7.3 Change the icon while selected (WinUI4K). */
public class IconWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("7.3 Selected icon (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WToggleButton play = new WToggleButton("Play", Symbol.PLAY);
            play.setSelectedIcon(Symbol.PAUSE);
            play.addItemListener(e -> play.setText(play.isSelected() ? "Pause" : "Play"));
            WToggleButton other = new WToggleButton("Play", Symbol.PLAY);
            other.setSelectedIcon(Symbol.PAUSE);

            frame.add(play);
            frame.add(other);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            play.setSelected(true); // select only the first button at startup
        });
    }
}
