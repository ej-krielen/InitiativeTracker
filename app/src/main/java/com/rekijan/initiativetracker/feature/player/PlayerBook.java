package com.rekijan.initiativetracker.feature.player;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.core.data.KeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.party.PartyNames;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The player characters on this device: each is one {@link CharacterModel} the player keeps up to
 * date and sends to a GM with Table sync. There is no limit and they are free. The character's
 * name is also its name in the profile list.
 * <p>
 * Saved in the app's SharedPreferences: the ids in order under {@link #KEY_IDS}, each character
 * as JSON under {@link #KEY_CHARACTER_PREFIX} + id. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerBook {

    static final String KEY_IDS = "player_ids";
    static final String KEY_CHARACTER_PREFIX = "player_character_";

    private static final Gson GSON = new Gson();
    private static final Type IDS_TYPE = new TypeToken<ArrayList<String>>() {
    }.getType();

    private final KeyValueStore values;
    private final List<String> ids = new ArrayList<>();

    public PlayerBook(KeyValueStore values) {
        this.values = values;
        String json = values.getString(KEY_IDS, null);
        if (json != null) {
            try {
                List<String> saved = GSON.fromJson(json, IDS_TYPE);
                if (saved != null) {
                    for (String id : saved) {
                        if (id != null && !ids.contains(id)) ids.add(id);
                    }
                }
            } catch (JsonParseException e) {
                // Unreadable list: start without player characters
            }
        }
    }

    /** @return The ids of the player characters, oldest first. Read-only. */
    public List<String> getIds() {
        return Collections.unmodifiableList(ids);
    }

    public boolean contains(String id) {
        return id != null && ids.contains(id);
    }

    /**
     * Makes a new player character: a PC with this name, 0 hp and no conditions.
     *
     * @return Its id, or null when the name is empty after cleaning.
     */
    public String create(String name) {
        String clean = PartyNames.clean(name);
        if (clean.isEmpty()) return null;
        String id = UUID.randomUUID().toString();
        CharacterModel character = new CharacterModel();
        character.setCharacterName(clean);
        character.setPC(true);
        ids.add(id);
        saveIds();
        save(id, character);
        return id;
    }

    /** @return The saved character; a nameless PC when it is missing or unreadable. */
    public CharacterModel load(String id) {
        String json = values.getString(KEY_CHARACTER_PREFIX + id, null);
        if (json != null) {
            try {
                CharacterModel character = GSON.fromJson(json, CharacterModel.class);
                if (character != null) return character;
            } catch (JsonParseException e) {
                // Unreadable: fall through to an empty character
            }
        }
        CharacterModel empty = new CharacterModel();
        empty.setPC(true);
        return empty;
    }

    /** Saves the character; ignored for an unknown id (e.g. one just deleted). */
    public void save(String id, CharacterModel character) {
        if (!contains(id)) return;
        values.edit().putString(KEY_CHARACTER_PREFIX + id, GSON.toJson(character)).apply();
    }

    /** @return The character's name, as shown in the profile list. */
    public String getName(String id) {
        return load(id).getCharacterName();
    }

    /** @return True when renamed; false for an unknown id or an empty name. */
    public boolean rename(String id, String name) {
        String clean = PartyNames.clean(name);
        if (!contains(id) || clean.isEmpty()) return false;
        CharacterModel character = load(id);
        character.setCharacterName(clean);
        save(id, character);
        return true;
    }

    /** Deletes the character and its saved data. */
    public void delete(String id) {
        if (!ids.remove(id)) return;
        saveIds();
        values.edit().remove(KEY_CHARACTER_PREFIX + id).apply();
    }

    private void saveIds() {
        values.edit().putString(KEY_IDS, GSON.toJson(ids, IDS_TYPE)).apply();
    }
}
