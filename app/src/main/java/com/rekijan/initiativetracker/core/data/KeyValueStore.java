package com.rekijan.initiativetracker.core.data;

/**
 * The few SharedPreferences calls the save code needs, as a plain-Java interface. The app uses
 * {@link PrefsKeyValueStore}; the unit tests use an in-memory map, so saving, loading and the
 * 1.x migration are tested without Android.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public interface KeyValueStore {

    /** A set of changes that is saved in one go by {@link #apply()}. */
    interface Editor {
        Editor putString(String key, String value);

        Editor putInt(String key, int value);

        Editor putBoolean(String key, boolean value);

        Editor remove(String key);

        /** Saves all changes at once. */
        void apply();
    }

    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    boolean contains(String key);

    Editor edit();
}
