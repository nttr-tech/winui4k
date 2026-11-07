package com.appkitbox.winui4k.swing_to_winui4k.jprogressbar.background;

import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** 11.5 Update from a background task (Swing). */
public class BackgroundSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("11.5 Background task (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JProgressBar progress = new JProgressBar(0, 100);
            JLabel status = new JLabel("Working...");
            new SwingWorker<Void, Integer>() {
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
