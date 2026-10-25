package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything Table sync shares about one player character, in both directions: name, initiative
 * and initiative bonus, hp and max hp, fast healing and regeneration, the free-text lines (AC,
 * saves, maneuvers, skills, attack routine, notes) and the conditions. The player sends it to ask
 * the GM for changes; the GM's copy ({@link GmCopy}) uses it to send the GM's version back.
 * {@link #sentAt} goes up on every send, so the GM's app can tell a new send from an old one.
 * <p>
 * Whatever comes in is cleaned up: numbers are kept inside the ranges of {@link CharacterModel},
 * texts are cut to the limits below and there are at most {@link #MAX_CONDITIONS} conditions, the
 * same limits the Firestore rules enforce. Plain Java, so the mapping and the limits are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerSheet {

    // Keep these in sync with firestore.rules and the maxLength of the name field
    public static final int MAX_NAME = 50;
    public static final int MAX_SHORT_TEXT = 200;
    public static final int MAX_LONG_TEXT = 1000;
    public static final int MAX_NOTES = 2000;
    public static final int MAX_CONDITIONS = 50;
    public static final int MAX_CONDITION_NAME = 100;
    public static final int MAX_CONDITION_DESCRIPTION = 500;

    // Firestore field names, also used in firestore.rules
    static final String KEY_NAME = "name";
    static final String KEY_INITIATIVE = "initiative";
    static final String KEY_INITIATIVE_BONUS = "initiativeBonus";
    static final String KEY_HP = "hp";
    static final String KEY_MAX_HP = "maxHp";
    static final String KEY_FAST_HEALING = "fastHealing";
    static final String KEY_REGENERATION = "regeneration";
    static final String KEY_AC = "ac";
    static final String KEY_SAVES = "saves";
    static final String KEY_MANEUVERS = "maneuvers";
    static final String KEY_SKILLS = "skills";
    static final String KEY_ATTACK_ROUTINE = "attackRoutine";
    static final String KEY_NOTES = "notes";
    static final String KEY_CONDITIONS = "conditions";
    static final String KEY_CONDITION_NAME = "name";
    static final String KEY_CONDITION_DURATION = "duration";
    static final String KEY_CONDITION_DESCRIPTION = "description";
    static final String KEY_SENT_AT = "sentAt";

    public String name = "";
    public int initiative;
    public int initiativeBonus;
    public int hp;
    public int maxHp;
    public int fastHealing;
    public int regeneration;
    public String ac = "";
    public String saves = "";
    public String maneuvers = "";
    public String skills = "";
    public String attackRoutine = "";
    public String notes = "";
    public List<DebuffModel> conditions = new ArrayList<>();
    /** When the player sent this, in ms; only compared with the same player's earlier sends. */
    public long sentAt;

    /** @return Everything Table sync shares about this character (sentAt still 0). */
    public static PlayerSheet of(CharacterModel character) {
        PlayerSheet s = new PlayerSheet();
        s.name = character.getCharacterName();
        s.initiative = character.getInitiative();
        s.initiativeBonus = character.getInitiativeBonus();
        s.hp = character.getHp();
        s.maxHp = character.getMaxHp();
        s.fastHealing = character.getFastHealing();
        s.regeneration = character.getRegeneration();
        s.ac = character.getAc();
        s.saves = character.getSaves();
        s.maneuvers = character.getManeuvers();
        s.skills = character.getSkills();
        s.attackRoutine = character.getAttackRoutine();
        s.notes = character.getCharacterNotes();
        s.conditions = new ArrayList<>(character.getDebuffList());
        return s.cleaned();
    }

    /** @return True when there is a name; a player can't send a nameless character. */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /** @return A copy with every number in range, every text trimmed and cut, and the conditions copied. */
    public PlayerSheet cleaned() {
        PlayerSheet s = new PlayerSheet();
        s.name = cut(name, MAX_NAME);
        s.initiative = NumberInput.clamp(initiative, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.initiativeBonus = NumberInput.clamp(initiativeBonus, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.hp = NumberInput.clamp(hp, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
        s.maxHp = NumberInput.clamp(maxHp, 0, CharacterModel.MAX_HP);
        s.fastHealing = NumberInput.clamp(fastHealing, 0, CharacterModel.MAX_HEALING);
        s.regeneration = NumberInput.clamp(regeneration, 0, CharacterModel.MAX_HEALING);
        s.ac = cut(ac, MAX_SHORT_TEXT);
        s.saves = cut(saves, MAX_SHORT_TEXT);
        s.maneuvers = cut(maneuvers, MAX_SHORT_TEXT);
        s.skills = cut(skills, MAX_LONG_TEXT);
        s.attackRoutine = cut(attackRoutine, MAX_LONG_TEXT);
        s.notes = cut(notes, MAX_NOTES);
        if (conditions != null) {
            for (DebuffModel d : conditions) {
                if (s.conditions.size() >= MAX_CONDITIONS) break;
                if (d != null) s.conditions.add(cleanedCondition(d.getName(), d.getDuration(), d.getDescription()));
            }
        }
        s.sentAt = Math.max(0, sentAt);
        return s;
    }

    /** @return The cleaned sheet as a Firestore document (numbers as Long), without the expiry. */
    public Map<String, Object> toMap() {
        PlayerSheet s = cleaned();
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_NAME, s.name);
        map.put(KEY_INITIATIVE, (long) s.initiative);
        map.put(KEY_INITIATIVE_BONUS, (long) s.initiativeBonus);
        map.put(KEY_HP, (long) s.hp);
        map.put(KEY_MAX_HP, (long) s.maxHp);
        map.put(KEY_FAST_HEALING, (long) s.fastHealing);
        map.put(KEY_REGENERATION, (long) s.regeneration);
        map.put(KEY_AC, s.ac);
        map.put(KEY_SAVES, s.saves);
        map.put(KEY_MANEUVERS, s.maneuvers);
        map.put(KEY_SKILLS, s.skills);
        map.put(KEY_ATTACK_ROUTINE, s.attackRoutine);
        map.put(KEY_NOTES, s.notes);
        List<Map<String, Object>> list = new ArrayList<>();
        for (DebuffModel d : s.conditions) {
            Map<String, Object> item = new HashMap<>();
            item.put(KEY_CONDITION_NAME, d.getName());
            item.put(KEY_CONDITION_DURATION, (long) d.getDuration());
            item.put(KEY_CONDITION_DESCRIPTION, d.getDescription());
            list.add(item);
        }
        map.put(KEY_CONDITIONS, list);
        map.put(KEY_SENT_AT, s.sentAt);
        return map;
    }

    /**
     * Reads a player's send. Like {@link #read}, and a send without a name counts as no send.
     *
     * @return The cleaned sheet, or null when there is no map or no name.
     */
    public static PlayerSheet fromMap(Map<String, Object> map) {
        PlayerSheet s = read(map);
        return s != null && s.hasName() ? s : null;
    }

    /**
     * Reads a received document. Fields of the wrong type count as missing, numbers are clamped,
     * texts cut and conditions limited, so a tampered document can't break a character.
     *
     * @return The cleaned sheet, or null when there is no map.
     */
    public static PlayerSheet read(Map<String, Object> map) {
        if (map == null) return null;
        PlayerSheet s = new PlayerSheet();
        s.name = text(map.get(KEY_NAME));
        s.initiative = whole(map.get(KEY_INITIATIVE));
        s.initiativeBonus = whole(map.get(KEY_INITIATIVE_BONUS));
        s.hp = whole(map.get(KEY_HP));
        s.maxHp = whole(map.get(KEY_MAX_HP));
        s.fastHealing = whole(map.get(KEY_FAST_HEALING));
        s.regeneration = whole(map.get(KEY_REGENERATION));
        s.ac = text(map.get(KEY_AC));
        s.saves = text(map.get(KEY_SAVES));
        s.maneuvers = text(map.get(KEY_MANEUVERS));
        s.skills = text(map.get(KEY_SKILLS));
        s.attackRoutine = text(map.get(KEY_ATTACK_ROUTINE));
        s.notes = text(map.get(KEY_NOTES));
        Object list = map.get(KEY_CONDITIONS);
        if (list instanceof List) {
            for (Object item : (List<?>) list) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) item;
                s.conditions.add(new DebuffModel(text(m.get(KEY_CONDITION_NAME)),
                        whole(m.get(KEY_CONDITION_DURATION)), text(m.get(KEY_CONDITION_DESCRIPTION))));
            }
        }
        s.sentAt = number(map.get(KEY_SENT_AT));
        return s.cleaned();
    }

    /**
     * Puts this sheet on a character: every shared field, and the conditions are replaced. Whether
     * it is a PC, its turn order and its sync id stay as they are.
     */
    public void applyTo(CharacterModel character) {
        PlayerSheet s = cleaned();
        character.setCharacterName(s.name);
        character.setInitiative(s.initiative);
        character.setInitiativeBonus(s.initiativeBonus);
        character.setMaxHp(s.maxHp);
        character.setHp(s.hp);
        character.setFastHealing(s.fastHealing);
        character.setRegeneration(s.regeneration);
        character.setAc(s.ac);
        character.setSaves(s.saves);
        character.setManeuvers(s.maneuvers);
        character.setSkills(s.skills);
        character.setAttackRoutine(s.attackRoutine);
        character.setCharacterNotes(s.notes);
        character.getDebuffList().clear();
        character.getDebuffList().addAll(s.conditions);
    }

    static DebuffModel cleanedCondition(String name, int duration, String description) {
        return new DebuffModel(cutRaw(name, MAX_CONDITION_NAME),
                NumberInput.clamp(duration, 0, DebuffModel.MAX_DURATION),
                cutRaw(description, MAX_CONDITION_DESCRIPTION));
    }

    private static String cut(String value, int max) {
        return cutRaw(value == null ? "" : value.trim(), max);
    }

    /** Cuts without trimming (a condition being typed may end in a space). */
    private static String cutRaw(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    /** @return The number as an int, kept inside the int range; cleaned() narrows it further. */
    private static int whole(Object value) {
        return NumberInput.clamp(number(value), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static long number(Object value) {
        // NaN becomes 0, infinities become the long limits
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
