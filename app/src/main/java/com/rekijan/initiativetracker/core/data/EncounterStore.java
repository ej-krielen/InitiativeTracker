package com.rekijan.initiativetracker.core.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.List;

/**
 * Saves and loads the current fight in SharedPreferences. Uses the same file and keys as 1.x,
 * so users keep their characters and round counter after updating.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class EncounterStore {

    // 1.x keys, keep them as they are
    private static final String PREFS_NAME = "SHARED_PREF_TAG";
    private static final String KEY_CHARACTERS = "GSON_TAG";
    private static final String KEY_ROUND = "ROUND_COUNTER";

    private final SharedPreferences prefs;

    public EncounterStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return The saved characters, or null when nothing was saved yet. */
    public List<CharacterModel> loadCharacters() {
        return EncounterJson.fromJson(prefs.getString(KEY_CHARACTERS, null));
    }

    public int loadRound() {
        return prefs.getInt(KEY_ROUND, 0);
    }

    public void save(Encounter encounter) {
        prefs.edit()
                .putString(KEY_CHARACTERS, EncounterJson.toJson(encounter.getCharacters()))
                .putInt(KEY_ROUND, encounter.getRound())
                .apply();
    }
}
