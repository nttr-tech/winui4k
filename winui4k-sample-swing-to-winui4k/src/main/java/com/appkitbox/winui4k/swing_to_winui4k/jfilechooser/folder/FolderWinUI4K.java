package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.folder;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFileChooser;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.io.File;

/** 27.5 Choose a folder (WinUI4K). */
public class FolderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("27.5 Folder (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("No folder chosen");
            WButton browse = new WButton("Browse...");
            browse.addActionListener(e -> {
                WFileChooser chooser = new WFileChooser(new File("files"));
                chooser.setDialogTitle("Choose the output folder");
                chooser.setFileSelectionMode(WFileChooser.DIRECTORIES_ONLY);
                chooser.setApproveButtonText("Select");
                if (chooser.showOpenDialog(frame) == WFileChooser.APPROVE_OPTION) {
                    status.setText("Output: " + chooser.getSelectedFile().getPath());
                }
            });

            frame.add(browse);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            browse.doClick();
        });
    }
}
