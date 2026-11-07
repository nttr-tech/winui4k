package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.filter;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFileChooser;
import com.appkitbox.winui4k.WFileNameExtensionFilter;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.io.File;

/** 27.3 Show only some kinds of files (WinUI4K). */
public class FilterWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("27.3 Filter (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("No file opened");
            WButton open = new WButton("Open...");
            open.addActionListener(e -> {
                WFileChooser chooser = new WFileChooser(new File("files"));
                chooser.setDialogTitle("Open a text file");
                chooser.setAcceptAllFileFilterUsed(false);   // remove "All Files"
                chooser.addChoosableFileFilter(new WFileNameExtensionFilter("Markdown files", "md"));
                chooser.addChoosableFileFilter(new WFileNameExtensionFilter("Text files", "txt", "csv"));  // selected
                if (chooser.showOpenDialog(frame) == WFileChooser.APPROVE_OPTION) {
                    status.setText("Opened " + chooser.getSelectedFile().getName());
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
