package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.background;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.concurrent.CompletableFuture;

/** 11.5 Update from a background task (WinUI4K). */
public class BackgroundWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.5 Background task (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressBar progress = new WProgressBar(0, 100);
            WLabel status = new WLabel("Working...");
            CompletableFuture.runAsync(() -> {
                for (int i = 1; i <= 100; i++) {
                    sleep(100);                                 // runs on another thread
                    int value = i;
                    WinUiUtilities.invokeLater(() -> progress.setValue(value));   // back to the UI thread
                }
            }).thenRun(() -> WinUiUtilities.invokeLater(() -> status.setText("Done")));

            frame.add(progress);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
