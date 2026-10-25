package com.rekijan.initiativetracker.core.data;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link KeyValueStore} for unit tests. Like SharedPreferences, an editor's changes
 * only land on {@link Editor#apply()}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class MemoryKeyValueStore implements KeyValueStore {

    private final Map<String, Object> map = new HashMap<>();

    @Override
    public String getString(String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Object value = map.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @Override
    public boolean contains(String key) {
        return map.containsKey(key);
    }

    /** @return How many keys are saved, to check that nothing was removed. */
    public int size() {
        return map.size();
    }

    @Override
    public Editor edit() {
        Map<String, Object> changes = new HashMap<>();
        return new Editor() {
            @Override
            public Editor putString(String key, String value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                changes.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                changes.put(key, null);
                return this;
            }

            @Override
            public void apply() {
                for (Map.Entry<String, Object> e : changes.entrySet()) {
                    if (e.getValue() == null) {
                        map.remove(e.getKey());
                    } else {
                        map.put(e.getKey(), e.getValue());
                    }
                }
            }
        };
    }
}
