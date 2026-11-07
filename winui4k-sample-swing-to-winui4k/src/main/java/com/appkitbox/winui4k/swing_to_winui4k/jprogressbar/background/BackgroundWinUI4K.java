package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.background;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WSwingWorker;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.List;

/** 11.5 Update from a background task (WinUI4K). */
public class BackgroundWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("11.5 Background task (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WProgressBar progress = new WProgressBar(0, 100);
            WLabel status = new WLabel("Working...");
            new WSwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    for (int i = 1; i <= 100; i++) {
                        Thread.sleep(100);                      // runs on another thread
                        publish(i);
                    }
                    return null;
                }

                @Override
                protected void process(List<Integer> chunks) {  // runs on the UI thread
                    progress.setValue(chunks.get(chunks.size() - 1));
                }

                @Override
                protected void done() {
                    status.setText("Done");
                }
            }.execute();

            frame.add(progress);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
