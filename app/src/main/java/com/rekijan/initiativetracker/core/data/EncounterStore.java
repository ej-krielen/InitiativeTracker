package com.rekijan.initiativetracker.core.data;

/**
 * The app-wide save file. Uses the same SharedPreferences file as 1.x ({@link #PREFS_NAME}), so
 * the 1.x save is still there for {@link com.rekijan.initiativetracker.feature.party.PartyStore}
 * to turn into the first party. The parties themselves are saved by PartyStore; this class keeps
 * the Remove Ads flag.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class EncounterStore {

    /** The 1.x SharedPreferences file, keep it as it is. */
    public static final String PREFS_NAME = "SHARED_PREF_TAG";
    // New in 2.0, same key as Mook Roller and Secret Roller
    private static final String KEY_ADS_REMOVED = "ads_removed";

    private final KeyValueStore values;

    public EncounterStore(KeyValueStore values) {
        this.values = values;
    }

    public boolean isAdsRemoved() {
        return values.getBoolean(KEY_ADS_REMOVED, false);
    }

    public void setAdsRemoved(boolean removed) {
        values.edit().putBoolean(KEY_ADS_REMOVED, removed).apply();
    }
}
