package us.poliscore.legiscan.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;

public class NoOpLegiscanCache implements LegiscanCache {
    @Override
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value) {
        // no-op
    }
}
