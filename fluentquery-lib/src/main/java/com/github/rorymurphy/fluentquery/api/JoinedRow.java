package com.github.rorymurphy.fluentquery.api;

import com.github.rorymurphy.fluentquery.meta.QEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JoinedRow {
    private final Map<String, Object> byAlias;

    public JoinedRow(Map<String, Object> byAlias) {
        this.byAlias = Collections.unmodifiableMap(new HashMap<>(byAlias));
    }

    public <T> T as(Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Object value : byAlias.values()) {
            if (type.isInstance(value)) {
                matches.add(type.cast(value));
            }
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No joined entity accessor present for type: " + type.getName());
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Multiple joined entity accessors present for type: " + type.getName()
                + ". Use as(alias, type) for disambiguation.");
        }
        return matches.get(0);
    }

    public <T> T as(String alias, Class<T> type) {
        Object value = byAlias.get(alias);
        if (value == null) {
            throw new IllegalArgumentException("No joined entity accessor present for alias: " + alias);
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Joined accessor for alias '" + alias + "' is not of expected type " + type.getName());
        }
        return type.cast(value);
    }

    @SuppressWarnings("unchecked")
    public <T extends QEntity<?>> T as(T entityAccessor) {
        return (T) as(entityAccessor.alias(), (Class<T>) entityAccessor.getClass());
    }

    public Map<String, Object> accessors() {
        return byAlias;
    }

    public static Map<String, Object> singletonAccessorMap(Object accessor) {
        Map<String, Object> byAlias = new HashMap<>();
        if (accessor instanceof JoinedRow joinedRow) {
            byAlias.putAll(joinedRow.byAlias);
            return byAlias;
        }
        if (accessor instanceof QEntity<?> qEntity) {
            byAlias.put(qEntity.alias(), qEntity);
            return byAlias;
        }
        byAlias.put(accessor.getClass().getName(), accessor);
        return byAlias;
    }
}
