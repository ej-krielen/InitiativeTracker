package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The GM's copy of a player's character: hp, max hp and conditions. The GM leads, so the GM's app
 * uploads this for every synced character in the hosted party, and the player's Sync replaces
 * those three on their own sheet with it. The rest of the player's sheet is theirs and is kept.
 * <p>
 * Whatever comes in is cleaned up by {@link #fromMap}: numbers kept in range, at most
 * {@link #MAX_CONDITIONS} conditions, texts cut to the limits below. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class GmCopy {

    // Keep in sync with firestore.rules
    public static final int MAX_CONDITIONS = 50;
    public static final int MAX_CONDITION_NAME = 100;
    public static final int MAX_CONDITION_DESCRIPTION = 500;

    static final String KEY_HP = "hp";
    static final String KEY_MAX_HP = "maxHp";
    static final String KEY_CONDITIONS = "conditions";
    static final String KEY_CONDITION_NAME = "name";
    static final String KEY_CONDITION_DURATION = "duration";
    static final String KEY_CONDITION_DESCRIPTION = "description";

    public int hp;
    public int maxHp;
    public final List<DebuffModel> conditions = new ArrayList<>();

    /** @return The GM's copy of this character, cleaned up. */
    public static GmCopy of(CharacterModel character) {
        GmCopy copy = new GmCopy();
        copy.hp = character.getHp();
        copy.maxHp = character.getMaxHp();
        for (DebuffModel d : character.getDebuffList()) {
            if (copy.conditions.size() >= MAX_CONDITIONS) break;
            copy.conditions.add(cleaned(d.getName(), d.getDuration(), d.getDescription()));
        }
        return copy;
    }

    /** @return The copy as a Firestore document (numbers as Long), without the expiry. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_HP, (long) clampHp(hp));
        map.put(KEY_MAX_HP, (long) clampMaxHp(maxHp));
        List<Map<String, Object>> list = new ArrayList<>();
        for (DebuffModel d : conditions) {
            if (list.size() >= MAX_CONDITIONS) break;
            DebuffModel c = cleaned(d.getName(), d.getDuration(), d.getDescription());
            Map<String, Object> item = new HashMap<>();
            item.put(KEY_CONDITION_NAME, c.getName());
            item.put(KEY_CONDITION_DURATION, (long) c.getDuration());
            item.put(KEY_CONDITION_DESCRIPTION, c.getDescription());
            list.add(item);
        }
        map.put(KEY_CONDITIONS, list);
        return map;
    }

    /**
     * Reads a received document. Wrong types count as missing, so a broken document can't break
     * the player's sheet.
     *
     * @return The cleaned copy, or null when there is no map.
     */
    public static GmCopy fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GmCopy copy = new GmCopy();
        copy.hp = clampHp(number(map.get(KEY_HP)));
        copy.maxHp = clampMaxHp(number(map.get(KEY_MAX_HP)));
        Object list = map.get(KEY_CONDITIONS);
        if (list instanceof List) {
            for (Object item : (List<?>) list) {
                if (copy.conditions.size() >= MAX_CONDITIONS) break;
                if (!(item instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) item;
                copy.conditions.add(cleaned(text(m.get(KEY_CONDITION_NAME)),
                        NumberInput.clamp(number(m.get(KEY_CONDITION_DURATION)), 0, DebuffModel.MAX_DURATION),
                        text(m.get(KEY_CONDITION_DESCRIPTION))));
            }
        }
        return copy;
    }

    /** Puts the GM's hp, max hp and conditions on the player's character; nothing else changes. */
    public void applyTo(CharacterModel character) {
        character.setMaxHp(maxHp);
        character.setHp(hp);
        character.getDebuffList().clear();
        for (DebuffModel d : conditions) {
            character.getDebuffList().add(cleaned(d.getName(), d.getDuration(), d.getDescription()));
        }
    }

    private static DebuffModel cleaned(String name, int duration, String description) {
        return new DebuffModel(cut(name, MAX_CONDITION_NAME),
                NumberInput.clamp(duration, 0, DebuffModel.MAX_DURATION),
                cut(description, MAX_CONDITION_DESCRIPTION));
    }

    private static int clampHp(long hp) {
        return NumberInput.clamp(hp, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
    }

    private static int clampMaxHp(long maxHp) {
        return NumberInput.clamp(maxHp, 0, CharacterModel.MAX_HP);
    }

    private static String cut(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
