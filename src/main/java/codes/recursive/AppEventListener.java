package codes.recursive;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class AppEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(AppEventListener.class);
    
    private final FileWatcherService fileWatcherService;

    public AppEventListener(FileWatcherService fileWatcherService) {
        this.fileWatcherService = fileWatcherService;
    }

    @EventListener
    void onStartup(ServerStartupEvent event) throws IOException, InterruptedException {
        fileWatcherService.watch();
    }
}
