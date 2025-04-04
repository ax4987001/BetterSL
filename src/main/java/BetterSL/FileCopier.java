package BetterSL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileCopier {
    private static final BlockingQueue<FileCopyTask> copyQueue = new LinkedBlockingQueue<>();
    private static Thread copyThread;

    public static void asyncCopy(Path sourcePath, Path targetPath) {
        copyQueue.add(new FileCopyTask(sourcePath, targetPath));
        ensureCopyThread();
    }

    private static synchronized void ensureCopyThread() {
        if (copyThread == null || !copyThread.isAlive()) {
            copyThread = new Thread(() -> {
                while (true) {
                    try {
                        FileCopyTask task = copyQueue.take();
                        task.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            copyThread.setDaemon(true); // 守护线程
            copyThread.start();
        }
    }

    private static class FileCopyTask {
        private final Path sourcePath;
        private final Path targetPath;

        public FileCopyTask(Path sourcePath, Path targetPath) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
        }

        public void execute() {
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File copied successfully from " + sourcePath + " to " + targetPath);
            } catch (IOException e) {
                System.err.println("Failed to copy file from " + sourcePath + " to " + targetPath);
                e.printStackTrace();
            }
        }
    }
}
