package us.poliscore.legiscan.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSystemLegiscanCache implements LegiscanCache {

    private static final Logger LOGGER = Logger.getLogger(FileSystemLegiscanCache.class.getName());

    private final File baseDir;
    private final ObjectMapper objectMapper;
    private final long defaultTtlMillis; // If > 0, applies to all entries unless overridden

    public FileSystemLegiscanCache(File baseDir, ObjectMapper objectMapper, long defaultTtlMillis) {
        this.baseDir = baseDir;
        this.objectMapper = objectMapper;
        this.defaultTtlMillis = defaultTtlMillis;

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IllegalStateException("Could not create cache directory: " + baseDir);
        }
    }

    public FileSystemLegiscanCache(File baseDir, ObjectMapper objectMapper) {
        this(baseDir, objectMapper, 0);
    }

    private File resolvePath(String key) {
        String safeName = key.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return new File(baseDir, safeName + ".json");
    }

    @Override
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        File file = resolvePath(key);
        if (!file.exists()) {
            return Optional.empty();
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            CachedEntry entry = objectMapper.readValue(data, CachedEntry.class);

            long now = Instant.now().toEpochMilli();
            if (entry.getTtlMillis() > 0 && now > entry.getTimestamp() + entry.getTtlMillis()) {
                LOGGER.fine("Cache expired for key: " + key);
                file.delete(); // Clean up expired file
                return Optional.empty();
            }

            T value = objectMapper.convertValue(entry.getValue(), typeRef);
            return Optional.of(value);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read cache for key: " + key, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtlMillis);
    }

    public void put(String key, Object value, long ttlMillis) {
        File file = resolvePath(key);
        try {
            CachedEntry entry = new CachedEntry(value, Instant.now().toEpochMilli(), ttlMillis);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, entry);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write cache for key: " + key, e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedEntry {
        private Object value;
        private long timestamp;
        private long ttlMillis;
    }
}
