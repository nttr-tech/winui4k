package com.appkitbox.winui4k.swing_to_winui4k.jlabel.icon;

import com.appkitbox.winui4k.HorizontalTextPosition;
import com.appkitbox.winui4k.VerticalTextPosition;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WImageIcon;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 4.4 Icon (WinUI4K). */
public class IconWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("4.4 Icon (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WImageIcon person = new WImageIcon("icons/person.png");
            WLabel user = new WLabel("Signed in as alice", person);
            WLabel avatar = new WLabel(person);                   // icon only
            WLabel profile = new WLabel("Profile", person);
            profile.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
            profile.setVerticalTextPosition(VerticalTextPosition.BOTTOM); // text below the icon

            frame.add(user);
            frame.add(avatar);
            frame.add(profile);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
