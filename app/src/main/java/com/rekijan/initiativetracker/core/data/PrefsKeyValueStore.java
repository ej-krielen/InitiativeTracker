package com.rekijan.initiativetracker.core.data;

import android.content.SharedPreferences;

/**
 * {@link KeyValueStore} on top of SharedPreferences, used by the app.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PrefsKeyValueStore implements KeyValueStore {

    private final SharedPreferences prefs;

    public PrefsKeyValueStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    @Override
    public boolean contains(String key) {
        return prefs.contains(key);
    }

    @Override
    public Editor edit() {
        SharedPreferences.Editor editor = prefs.edit();
        return new Editor() {
            @Override
            public Editor putString(String key, String value) {
                editor.putString(key, value);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                editor.putInt(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                editor.putBoolean(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                editor.remove(key);
                return this;
            }

            @Override
            public void apply() {
                editor.apply();
            }
        };
    }
}
