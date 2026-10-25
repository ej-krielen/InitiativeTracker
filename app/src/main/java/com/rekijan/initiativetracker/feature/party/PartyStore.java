package com.rekijan.initiativetracker.feature.party;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.core.data.KeyValueStore;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Saves the parties in the app's save file (the 1.x SharedPreferences file): the party list, which
 * party is active, the owned party pack level, and per party its characters (as JSON, in the 1.x
 * format) and round counter, under keys that contain the party id.
 * <p>
 * 1.x kept one fight under GSON_TAG and ROUND_COUNTER. On the first start of 2.0,
 * {@link #migrateIfNeeded} copies those into the first party, once, in a single write together
 * with the "migrated" flag. The 1.x keys are left in place as a safety net. Plain Java (through
 * {@link KeyValueStore}), so the migration is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartyStore {

    // 1.x keys: read once by the migration, never written or removed
    static final String KEY_1X_CHARACTERS = "GSON_TAG";
    static final String KEY_1X_ROUND = "ROUND_COUNTER";

    private static final String KEY_PARTIES = "parties";
    private static final String KEY_ACTIVE_PARTY_ID = "active_party_id";
    private static final String KEY_PACK_LEVEL = "party_pack_level";
    private static final String KEY_MIGRATED = "parties_migrated";
    private static final String KEY_CHARACTERS_PREFIX = "party_characters_";
    private static final String KEY_ROUND_PREFIX = "party_round_";

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Party>>() {
    }.getType();

    private final KeyValueStore values;

    /**
     * @param firstPartyName Name for the party made from the 1.x save, or the first party of a
     *                       fresh install, in the user's language.
     */
    public PartyStore(KeyValueStore values, String firstPartyName) {
        this.values = values;
        migrateIfNeeded(firstPartyName);
    }

    /** @return The saved parties in their saved order; empty when the list is missing or unreadable. */
    public List<Party> loadParties() {
        List<Party> parties = new ArrayList<>();
        String json = values.getString(KEY_PARTIES, null);
        if (json == null) return parties;
        try {
            List<Party> saved = GSON.fromJson(json, LIST_TYPE);
            if (saved != null) {
                for (Party p : saved) {
                    if (p != null && p.getId() != null) parties.add(p);
                }
            }
        } catch (JsonParseException e) {
            // Unreadable list: PartyBook starts a new one, the party data itself stays saved
        }
        return parties;
    }

    public void saveParties(List<Party> parties) {
        values.edit().putString(KEY_PARTIES, GSON.toJson(new ArrayList<>(parties), LIST_TYPE)).apply();
    }

    public String loadActiveId() {
        return values.getString(KEY_ACTIVE_PARTY_ID, null);
    }

    public void saveActiveId(String id) {
        values.edit().putString(KEY_ACTIVE_PARTY_ID, id).apply();
    }

    /** @return The party's characters as JSON (1.x format), or null when none were saved. */
    public String loadCharactersJson(String partyId) {
        return values.getString(charactersKey(partyId), null);
    }

    public int loadRound(String partyId) {
        return values.getInt(roundKey(partyId), 0);
    }

    public void saveFight(String partyId, String charactersJson, int round) {
        values.edit()
                .putString(charactersKey(partyId), charactersJson)
                .putInt(roundKey(partyId), round)
                .apply();
    }

    public void deleteFight(String partyId) {
        values.edit().remove(charactersKey(partyId)).remove(roundKey(partyId)).apply();
    }

    public int loadPackLevel() {
        return PartySlots.clampLevel(values.getInt(KEY_PACK_LEVEL, 0));
    }

    public void savePackLevel(int level) {
        values.edit().putInt(KEY_PACK_LEVEL, PartySlots.clampLevel(level)).apply();
    }

    static String charactersKey(String partyId) {
        return KEY_CHARACTERS_PREFIX + partyId;
    }

    static String roundKey(String partyId) {
        return KEY_ROUND_PREFIX + partyId;
    }

    /**
     * Turns the 1.x save into the first party, once. A fresh install gets an empty first party
     * (PartyBook fills it with the starting PCs). The 1.x JSON is copied as it is, without
     * parsing, so even a save this version can't read is kept.
     */
    private void migrateIfNeeded(String firstPartyName) {
        if (values.getBoolean(KEY_MIGRATED, false)) return;
        Party first = new Party(Objects.requireNonNull(firstPartyName));
        List<Party> parties = new ArrayList<>();
        parties.add(first);

        KeyValueStore.Editor editor = values.edit();
        String oldCharacters = values.getString(KEY_1X_CHARACTERS, null);
        if (oldCharacters != null) editor.putString(charactersKey(first.getId()), oldCharacters);
        if (values.contains(KEY_1X_ROUND)) {
            editor.putInt(roundKey(first.getId()), values.getInt(KEY_1X_ROUND, 0));
        }
        editor.putString(KEY_PARTIES, GSON.toJson(parties, LIST_TYPE))
                .putString(KEY_ACTIVE_PARTY_ID, first.getId())
                .putBoolean(KEY_MIGRATED, true)
                .apply();
    }
}
