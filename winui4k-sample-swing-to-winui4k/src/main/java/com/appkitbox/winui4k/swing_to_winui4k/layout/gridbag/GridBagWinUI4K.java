package com.appkitbox.winui4k.swing_to_winui4k.layout.gridbag;

import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.6 GridBagLayout (WinUI4K). */
public class GridBagWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.6 GridBagLayout (WinUI4K)");

            WGrid grid = new WGrid(8, 8);                // row spacing, column spacing
            grid.setMargin(4);
            grid.addColumn(GridLength.AUTO);             // column 0: labels at their own width
            grid.addColumn(GridLength.star());           // column 1: the rest of the width (weightx = 1)
            grid.addRow();                               // rows 0 and 1: their own height
            grid.addRow();
            grid.addRow(GridLength.star());              // row 2: the rest of the height (weighty = 1)

            WLabel name = new WLabel("Name:");
            name.setVerticalAlignment(VerticalAlignment.CENTER);
            grid.add(name, 0, 0);                        // (row, column): the reverse of gridx / gridy
            WLabel email = new WLabel("Email:");
            email.setVerticalAlignment(VerticalAlignment.CENTER);
            grid.add(email, 1, 0);

            grid.add(new WTextField(), 0, 1);            // fills the cell (fill = BOTH) by default
            grid.add(new WTextField(), 1, 1);

            WButton submit = new WButton("Submit");
            submit.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            submit.setVerticalAlignment(VerticalAlignment.TOP);
            grid.add(submit, 2, 0, 1, 2);                // row span 1, column span 2 (gridwidth = 2)

            frame.setContentPane(grid);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
