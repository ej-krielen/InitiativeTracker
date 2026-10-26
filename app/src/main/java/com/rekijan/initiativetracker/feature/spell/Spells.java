package com.rekijan.initiativetracker.feature.spell;

import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Casting a spell: one condition per target, all with the same caster and spell id, so they count
 * down on the caster's turn ({@link Encounter#nextTurn}) and can be ended together. Plain Java,
 * so the rules are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2026
 */
public final class Spells {

    // The same limits Table sync uses for a condition (PlayerSheet)
    public static final int MAX_NAME = 100;
    public static final int MAX_DESCRIPTION = 500;

    private Spells() {
    }

    /**
     * Puts the spell as a condition on every target and tells the encounter.
     *
     * @param duration In rounds, kept between 0 (no timer) and {@link DebuffModel#MAX_DURATION}.
     * @return The new spell's id, or null when there are no targets.
     */
    public static String cast(Encounter encounter, CharacterModel caster, List<CharacterModel> targets,
                              String name, int duration, String description) {
        if (targets.isEmpty()) return null;
        String spellId = UUID.randomUUID().toString();
        String cleanName = cut(name == null ? "" : name.trim(), MAX_NAME);
        String cleanDescription = cut(description == null ? "" : description.trim(), MAX_DESCRIPTION);
        int rounds = NumberInput.clamp(duration, 0, DebuffModel.MAX_DURATION);
        for (CharacterModel target : targets) {
            DebuffModel d = new DebuffModel(cleanName, rounds, cleanDescription);
            d.setCaster(caster.getStableId(), caster.getCharacterName(), spellId);
            target.getDebuffList().add(d);
        }
        encounter.notifyChanged();
        return spellId;
    }

    /**
     * Reads the duration field of Cast spell.
     *
     * @return 0 for empty (no timer), the rounds for 1 to {@link DebuffModel#MAX_DURATION}, or -1
     * when it is something else, so the dialog can say what fits.
     */
    public static int parseDuration(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return 0;
        if (!t.matches("\\d{1,9}")) return -1;
        int rounds = Integer.parseInt(t);
        return rounds >= 1 && rounds <= DebuffModel.MAX_DURATION ? rounds : -1;
    }

    /** @return The characters that can cast or be a target: everyone but spell cards. */
    public static List<CharacterModel> combatants(Encounter encounter) {
        List<CharacterModel> list = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            if (!c.isSpellCard()) list.add(c);
        }
        return list;
    }

    /** @return The characters that have this spell, in turn order. */
    public static List<CharacterModel> targetsOf(Encounter encounter, String spellId) {
        List<CharacterModel> targets = new ArrayList<>();
        if (spellId == null) return targets;
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (spellId.equals(d.getSpellId())) {
                    targets.add(c);
                    break;
                }
            }
        }
        return targets;
    }

    /**
     * Ends a spell early (dismissed, concentration lost): it comes off every target. A spell card
     * that only kept this spell going leaves the fight too.
     */
    public static void end(Encounter encounter, String spellId) {
        if (spellId == null) return;
        List<String> casters = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            List<DebuffModel> list = c.getDebuffList();
            for (int i = list.size() - 1; i >= 0; i--) {
                DebuffModel d = list.get(i);
                if (!spellId.equals(d.getSpellId())) continue;
                if (d.getCasterId() != null && !casters.contains(d.getCasterId())) casters.add(d.getCasterId());
                list.remove(i);
            }
        }
        List<CharacterModel> endedCards = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            if (c.isSpellCard() && casters.contains(c.getStableId()) && !encounter.hasSpellsOf(c)) endedCards.add(c);
        }
        if (endedCards.isEmpty()) {
            encounter.notifyChanged();
        } else {
            encounter.removeAll(endedCards);
        }
    }

    /** @return The spell id of the conditions a spell card keeps going, or null. */
    public static String spellOfCard(Encounter encounter, CharacterModel card) {
        String id = card.getStableId();
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (id.equals(d.getCasterId())) return d.getSpellId();
            }
        }
        return null;
    }

    private static String cut(String value, int max) {
        if (value.length() <= max) return value;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }
}
