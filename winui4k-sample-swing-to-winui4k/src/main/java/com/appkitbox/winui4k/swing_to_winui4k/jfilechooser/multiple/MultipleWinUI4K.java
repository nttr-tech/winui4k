package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.multiple;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFileChooser;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.io.File;

/** 27.6 Choose several files at once (WinUI4K). */
public class MultipleWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("27.6 Multiple (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("No files added");
            WButton add = new WButton("Add Files...");
            add.addActionListener(e -> {
                WFileChooser chooser = new WFileChooser(new File("files"));
                chooser.setDialogTitle("Add files");
                chooser.setMultiSelectionEnabled(true);
                if (chooser.showOpenDialog(frame) == WFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();
                    status.setText("Added " + files.length + " files");
                }
            });

            frame.add(add);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            add.doClick();
        });
    }
}
