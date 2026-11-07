package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.model;

import com.appkitbox.winui4k.WBoundedRangeModel;
import com.appkitbox.winui4k.WDefaultBoundedRangeModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WinUiUtilities;

/** 11.6 Share a model with a slider (WinUI4K). */
public class ModelWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.6 Shared model (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WBoundedRangeModel model = new WDefaultBoundedRangeModel(30, 0, 0, 100);
            WSlider slider = new WSlider(model);
            WProgressBar progress = new WProgressBar(model);

            frame.add(slider);
            frame.add(progress);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            model.setValue(75);
        });
    }
}
