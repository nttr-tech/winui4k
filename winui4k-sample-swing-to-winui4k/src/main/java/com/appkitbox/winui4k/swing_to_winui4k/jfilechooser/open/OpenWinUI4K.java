package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.open;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFileChooser;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.io.File;

/** 27.2 Choose a file to open (WinUI4K). */
public class OpenWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("27.2 Open (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("No file opened");
            WButton open = new WButton("Open...");
            open.addActionListener(e -> {
                WFileChooser chooser = new WFileChooser(new File("files"));
                chooser.setDialogTitle("Open a file");
                if (chooser.showOpenDialog(frame) == WFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    status.setText("Opened " + file.getName());
                }
            });

            frame.add(open);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            open.doClick();
        });
    }
}
