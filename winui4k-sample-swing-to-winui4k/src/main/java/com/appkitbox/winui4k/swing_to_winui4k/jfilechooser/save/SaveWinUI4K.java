package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.save;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFileChooser;
import com.appkitbox.winui4k.WFileNameExtensionFilter;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.io.File;

/** 27.4 Choose where to save (WinUI4K). */
public class SaveWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("27.4 Save (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel("Not saved");
            WButton save = new WButton("Save As...");
            save.addActionListener(e -> {
                WFileChooser chooser = new WFileChooser(new File("files"));
                chooser.setDialogTitle("Save the report");
                chooser.setFileFilter(new WFileNameExtensionFilter("Text files", "txt"));
                chooser.setSelectedFile(new File("report.txt"));   // suggested name
                if (chooser.showSaveDialog(frame) == WFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    status.setText("Saved to " + file.getPath());
                }
            });

            frame.add(save);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            save.doClick();
        });
    }
}
