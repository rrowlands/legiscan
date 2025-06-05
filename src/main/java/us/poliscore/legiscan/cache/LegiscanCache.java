package us.poliscore.legiscan.cache;

import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import us.poliscore.legiscan.view.LegiscanResponse;

public interface LegiscanCache {
    <T> Optional<T> get(String key, TypeReference<T> typeRef);
    Optional<LegiscanResponse> get(String key);
    void put(String key, Object value);
    void put(String key, Object value, long ttlSecs);
	void remove(String cacheKey);
}
