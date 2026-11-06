package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WListSelectionModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.6 Allowing multiple selection (WinUI4K). */
public class MultipleWinUI4K {
    private static void openFile(String name) {
        System.out.println("Open: " + name);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.6 Allowing multiple selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            String[] names = {"Report.docx", "Budget.xlsx", "Presentation.pptx", "Photo.png", "Memo.txt", "Minutes.md"};
            WList files = new WList(names);
            files.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);   // The default is single selection, so specify it
            WButton open = new WButton("Open");
            open.addActionListener(e -> {
                for (Object name : files.getSelectedValuesList()) openFile((String) name);
            });

            files.setWidth(200);
            files.setHeight(150);
            frame.add(files);
            frame.add(open);

            files.setSelectionInterval(1, 2);
            files.addSelectionInterval(4, 4);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
