package ru.test.avatarstub;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class AvatarPoolService {

    private final Path avatarDir;
    private final AtomicLong requestCounter = new AtomicLong(0);

    private volatile List<Path> avatars = List.of();

    public AvatarPoolService(AvatarProperties properties) {
        this.avatarDir = Paths.get(properties.getDir());
        reloadPool();
    }

    public synchronized void reloadPool() {
        try {
            Files.createDirectories(avatarDir);

            try (Stream<Path> stream = Files.list(avatarDir)) {
                this.avatars = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".jpeg");
                        })
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }

            System.out.println("Avatar pool loaded. Directory: "
                    + avatarDir.toAbsolutePath()
                    + ", files: " + avatars.size());

        } catch (IOException e) {
            throw new IllegalStateException("Cannot load avatar pool from " + avatarDir.toAbsolutePath(), e);
        }
    }

    public Path nextAvatar() {
        List<Path> currentPool = avatars;

        if (currentPool.isEmpty()) {
            throw new AvatarPoolEmptyException("Avatar pool is empty. Generate avatars first.");
        }

        long current = requestCounter.getAndIncrement();
        int index = Math.floorMod(current, currentPool.size());

        return currentPool.get(index);
    }

    public int size() {
        return avatars.size();
    }

    public long nextIndex() {
        List<Path> currentPool = avatars;

        if (currentPool.isEmpty()) {
            return 0;
        }

        return Math.floorMod(requestCounter.get(), currentPool.size());
    }

    public Path avatarDir() {
        return avatarDir;
    }
}
