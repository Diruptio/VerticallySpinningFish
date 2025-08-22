package diruptio.verticallyspinningfish;

import java.io.IOException;
import java.nio.file.*;

public class GroupsWatchThread implements Runnable {
    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path.of("groups").register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                }
                key.reset();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } catch (InterruptedException ignored) {}
    }
}
