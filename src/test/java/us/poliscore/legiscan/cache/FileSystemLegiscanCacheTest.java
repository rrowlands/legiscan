package us.poliscore.legiscan.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileSystemLegiscanCacheTest {

    private File tempDir;
    private FileSystemLegiscanCache cache;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setup() throws Exception {
        objectMapper = new ObjectMapper();
        tempDir = Files.createTempDirectory("legiscan-cache-test").toFile();
    }

    @AfterEach
    void cleanUp() {
        for (File file : tempDir.listFiles()) {
            file.delete();
        }
    }

    @AfterAll
    void tearDown() {
        tempDir.delete();
    }

    @Test
    void testPutAndGetWithoutTtl() {
        cache = new FileSystemLegiscanCache(tempDir, objectMapper);
        String key = "testKey";
        Map<String, String> value = Map.of("foo", "bar");

        cache.put(key, value);

        Optional<Map<String, String>> result = cache.get(key, new TypeReference<>() {});
        assertTrue(result.isPresent());
        assertEquals("bar", result.get().get("foo"));
    }

    @Test
    void testExpiredEntryIsRemoved() throws InterruptedException {
        long ttl = 100; // 100 ms
        cache = new FileSystemLegiscanCache(tempDir, objectMapper, ttl);
        String key = "shortLived";
        Map<String, String> value = Map.of("hello", "world");

        cache.put(key, value);
        Thread.sleep(150); // Wait for TTL to expire

        Optional<Map<String, String>> result = cache.get(key, new TypeReference<>() {});
        assertTrue(result.isEmpty(), "Expected cache to expire and return empty");

        File[] files = tempDir.listFiles();
        assertEquals(0, files.length, "Expected expired cache file to be deleted");
    }

    @Test
    void testOverwriteValue() {
        cache = new FileSystemLegiscanCache(tempDir, objectMapper);
        String key = "overwriteTest";
        Map<String, String> first = Map.of("a", "1");
        Map<String, String> second = Map.of("a", "2");

        cache.put(key, first);
        cache.put(key, second);

        Optional<Map<String, String>> result = cache.get(key, new TypeReference<>() {});
        assertTrue(result.isPresent());
        assertEquals("2", result.get().get("a"));
    }

    @Test
    void testCustomTtlOnPut() throws InterruptedException {
        cache = new FileSystemLegiscanCache(tempDir, objectMapper);
        String key = "customTtl";
        Map<String, String> value = Map.of("life", "short");

        cache.put(key, value, 100); // 100ms TTL
        Thread.sleep(150);

        Optional<Map<String, String>> result = cache.get(key, new TypeReference<>() {});
        assertTrue(result.isEmpty(), "Expected expired custom TTL value to be absent");
    }
}
