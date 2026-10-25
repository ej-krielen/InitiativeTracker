package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.HashMap;
import java.util.Map;

/**
 * The character stats a player sends to the GM's Table sync session: name, initiative and
 * initiative bonus, hp and max hp, and the free-text lines (AC, saves, maneuvers, skills, attack
 * routine, notes). {@link #sentAt} goes up on every send, so the GM's app can tell a new send
 * from an unchanged one.
 * <p>
 * Everything that comes in is cleaned up by {@link #fromMap}: numbers are kept inside the ranges
 * of {@link CharacterModel} and text is cut to the limits below, the same limits the Firestore
 * rules enforce. Plain Java, so the mapping and the limits are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerSheet {

    // Keep these in sync with firestore.rules and the maxLength of the fields in fragment_sync.xml
    public static final int MAX_NAME = 50;
    public static final int MAX_SHORT_TEXT = 200;
    public static final int MAX_LONG_TEXT = 1000;
    public static final int MAX_NOTES = 2000;

    // Firestore field names, also used in firestore.rules
    static final String KEY_NAME = "name";
    static final String KEY_INITIATIVE = "initiative";
    static final String KEY_INITIATIVE_BONUS = "initiativeBonus";
    static final String KEY_HP = "hp";
    static final String KEY_MAX_HP = "maxHp";
    static final String KEY_AC = "ac";
    static final String KEY_SAVES = "saves";
    static final String KEY_MANEUVERS = "maneuvers";
    static final String KEY_SKILLS = "skills";
    static final String KEY_ATTACK_ROUTINE = "attackRoutine";
    static final String KEY_NOTES = "notes";
    static final String KEY_SENT_AT = "sentAt";

    public String name = "";
    public int initiative;
    public int initiativeBonus;
    public int hp;
    public int maxHp;
    public String ac = "";
    public String saves = "";
    public String maneuvers = "";
    public String skills = "";
    public String attackRoutine = "";
    public String notes = "";
    /** When the player sent this, in ms; only compared with the same player's earlier sends. */
    public long sentAt;

    /** @return True when there is a name to send; everything else may be left empty. */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /** @return A copy with every number in range and every text trimmed and cut to its limit. */
    public PlayerSheet cleaned() {
        PlayerSheet s = new PlayerSheet();
        s.name = cut(name, MAX_NAME);
        s.initiative = NumberInput.clamp(initiative, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.initiativeBonus = NumberInput.clamp(initiativeBonus, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.hp = NumberInput.clamp(hp, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
        s.maxHp = NumberInput.clamp(maxHp, 0, CharacterModel.MAX_HP);
        s.ac = cut(ac, MAX_SHORT_TEXT);
        s.saves = cut(saves, MAX_SHORT_TEXT);
        s.maneuvers = cut(maneuvers, MAX_SHORT_TEXT);
        s.skills = cut(skills, MAX_LONG_TEXT);
        s.attackRoutine = cut(attackRoutine, MAX_LONG_TEXT);
        s.notes = cut(notes, MAX_NOTES);
        s.sentAt = Math.max(0, sentAt);
        return s;
    }

    /** @return The cleaned fields as a Firestore document (numbers as Long), without the expiry. */
    public Map<String, Object> toMap() {
        PlayerSheet s = cleaned();
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_NAME, s.name);
        map.put(KEY_INITIATIVE, (long) s.initiative);
        map.put(KEY_INITIATIVE_BONUS, (long) s.initiativeBonus);
        map.put(KEY_HP, (long) s.hp);
        map.put(KEY_MAX_HP, (long) s.maxHp);
        map.put(KEY_AC, s.ac);
        map.put(KEY_SAVES, s.saves);
        map.put(KEY_MANEUVERS, s.maneuvers);
        map.put(KEY_SKILLS, s.skills);
        map.put(KEY_ATTACK_ROUTINE, s.attackRoutine);
        map.put(KEY_NOTES, s.notes);
        map.put(KEY_SENT_AT, s.sentAt);
        return map;
    }

    /**
     * Reads a received Firestore document. Fields of the wrong type count as missing, numbers are
     * clamped and texts cut, so a tampered document can't break the GM's party.
     *
     * @return The cleaned sheet, or null when there is no map or no name.
     */
    public static PlayerSheet fromMap(Map<String, Object> map) {
        if (map == null) return null;
        PlayerSheet s = new PlayerSheet();
        s.name = text(map, KEY_NAME);
        s.initiative = whole(map, KEY_INITIATIVE);
        s.initiativeBonus = whole(map, KEY_INITIATIVE_BONUS);
        s.hp = whole(map, KEY_HP);
        s.maxHp = whole(map, KEY_MAX_HP);
        s.ac = text(map, KEY_AC);
        s.saves = text(map, KEY_SAVES);
        s.maneuvers = text(map, KEY_MANEUVERS);
        s.skills = text(map, KEY_SKILLS);
        s.attackRoutine = text(map, KEY_ATTACK_ROUTINE);
        s.notes = text(map, KEY_NOTES);
        s.sentAt = number(map, KEY_SENT_AT);
        PlayerSheet cleaned = s.cleaned();
        return cleaned.hasName() ? cleaned : null;
    }

    /** Copies the sheet onto a character, as a PC; (de)buffs, healing and turn order are kept. */
    public void applyTo(CharacterModel character) {
        PlayerSheet s = cleaned();
        character.setCharacterName(s.name);
        character.setInitiative(s.initiative);
        character.setInitiativeBonus(s.initiativeBonus);
        character.setHp(s.hp);
        character.setMaxHp(s.maxHp);
        character.setAc(s.ac);
        character.setSaves(s.saves);
        character.setManeuvers(s.maneuvers);
        character.setSkills(s.skills);
        character.setAttackRoutine(s.attackRoutine);
        character.setCharacterNotes(s.notes);
        character.setPC(true);
    }

    private static String cut(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= max) return trimmed;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(trimmed.charAt(max - 1)) ? max - 1 : max;
        return trimmed.substring(0, end);
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : "";
    }

    /** @return The number as an int, kept inside the int range; cleaned() narrows it further. */
    private static int whole(Map<String, Object> map, String key) {
        return NumberInput.clamp(number(map, key), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        // NaN becomes 0, infinities become the long limits
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
