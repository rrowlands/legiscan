package us.poliscore.legiscan.cache;

import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import us.poliscore.legiscan.view.LegiscanResponse;

public class NoOpLegiscanCache implements LegiscanCache {
    @Override
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        return Optional.empty();
    }
    
    @Override
    public Optional<LegiscanResponse> get(String key) {
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value) {
        // no-op
    }

	@Override
	public void put(String key, Object value, long ttlSecs) {
		// no-op
	}
	
	@Override
	public String toString() {
		return "NO OP Cache";
	}

	@Override
	public void remove(String cacheKey) {
		// no-op
	}

	@Override
	public boolean containsKey(String key) {
		return false;
	}
}
