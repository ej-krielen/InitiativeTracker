package com.rekijan.initiativetracker.core.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts the character list to and from the JSON format that 1.x stored, so old saves load in
 * 2.0. Plain Java, so that compatibility is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class EncounterJson {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<CharacterModel>>() {
    }.getType();

    private EncounterJson() {
    }

    public static String toJson(List<CharacterModel> characters) {
        return GSON.toJson(new ArrayList<>(characters), LIST_TYPE);
    }

    /**
     * @return The saved characters, or null when nothing (readable) was saved.
     */
    public static List<CharacterModel> fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            List<CharacterModel> characters = GSON.fromJson(json, LIST_TYPE);
            if (characters == null) return null;
            characters.removeIf(Objects::isNull);
            return characters;
        } catch (JsonParseException e) {
            return null;
        }
    }
}
