package us.poliscore.legiscan.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;

public interface LegiscanCache {
    <T> Optional<T> get(String key, TypeReference<T> typeRef);
    void put(String key, Object value);
    void put(String key, Object value, long ttlSecs);
}
