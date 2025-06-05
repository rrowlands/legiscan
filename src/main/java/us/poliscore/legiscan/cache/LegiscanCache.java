package us.poliscore.legiscan.cache;

import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import us.poliscore.legiscan.view.LegiscanResponse;

public interface LegiscanCache {
    public <T> Optional<T> get(String key, TypeReference<T> typeRef);
    public Optional<LegiscanResponse> get(String key);
    public void put(String key, Object value);
    public void put(String key, Object value, long ttlSecs);
    
    public boolean containsKey(String key);
	public void remove(String cacheKey);
}
