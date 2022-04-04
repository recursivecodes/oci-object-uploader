package codes.recursive;

import io.micronaut.runtime.Micronaut;

public class Application {

    private final FileWatcherService fileWatcherService;

    public Application(FileWatcherService fileWatcherService) {
        this.fileWatcherService = fileWatcherService;
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

}
