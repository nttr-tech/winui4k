package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.append;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** 12.4 Append lines to a log (WinUI4K). */
public class AppendWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("12.4 Append to a log (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane log = new WTextPane(6, 30);
            log.setEditable(false);
            WButton add = new WButton("Add log");
            int[] count = {0};
            add.addActionListener(e -> {
                count[0]++;
                log.append(LocalTime.of(9, 0, count[0]).format(DateTimeFormatter.ISO_LOCAL_TIME)
                        + " Step " + count[0] + " finished\n");
                log.setCaretPosition(log.getLength());                 // scroll to the end
            });

            frame.add(log);
            frame.add(add);

            frame.setSize(520, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            for (int i = 0; i < 10; i++) {
                add.doClick();
            }
        });
    }
}
