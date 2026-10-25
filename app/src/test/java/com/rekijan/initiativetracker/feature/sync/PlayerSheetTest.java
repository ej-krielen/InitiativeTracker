package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link PlayerSheet}: a sheet survives the trip to Firestore and back, incoming numbers and
 * texts are kept inside the limits, wrong types count as missing, and applying a sheet keeps the
 * character's (de)buffs and turn order.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerSheetTest {

    static PlayerSheet valeros() {
        PlayerSheet s = new PlayerSheet();
        s.name = "Valeros";
        s.initiative = 18;
        s.initiativeBonus = 2;
        s.hp = 30;
        s.maxHp = 34;
        s.ac = "18, touch 12";
        s.saves = "F+8 R+4 W+3";
        s.maneuvers = "CMB +9";
        s.skills = "Climb +8";
        s.attackRoutine = "Longsword +10 (1d8+5)";
        s.notes = "Power attack";
        s.sentAt = 1000;
        return s;
    }

    @Test
    public void roundTripKeepsEverything() {
        PlayerSheet back = PlayerSheet.fromMap(valeros().toMap());
        assertEquals("Valeros", back.name);
        assertEquals(18, back.initiative);
        assertEquals(2, back.initiativeBonus);
        assertEquals(30, back.hp);
        assertEquals(34, back.maxHp);
        assertEquals("18, touch 12", back.ac);
        assertEquals("F+8 R+4 W+3", back.saves);
        assertEquals("CMB +9", back.maneuvers);
        assertEquals("Climb +8", back.skills);
        assertEquals("Longsword +10 (1d8+5)", back.attackRoutine);
        assertEquals("Power attack", back.notes);
        assertEquals(1000, back.sentAt);
    }

    @Test
    public void numbersAreSentAsLong() {
        Map<String, Object> map = valeros().toMap();
        assertTrue(map.get(PlayerSheet.KEY_HP) instanceof Long);
        assertTrue(map.get(PlayerSheet.KEY_SENT_AT) instanceof Long);
    }

    @Test
    public void incomingNumbersAreClamped() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_INITIATIVE, 5000L);
        map.put(PlayerSheet.KEY_INITIATIVE_BONUS, Long.MIN_VALUE);
        map.put(PlayerSheet.KEY_HP, 1e12);
        map.put(PlayerSheet.KEY_MAX_HP, -5L);
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(CharacterModel.MAX_INITIATIVE, s.initiative);
        assertEquals(CharacterModel.MIN_INITIATIVE, s.initiativeBonus);
        assertEquals(CharacterModel.MAX_HP, s.hp);
        assertEquals(0, s.maxHp);
    }

    @Test
    public void incomingTextsAreCut() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_NAME, repeat('n', 80));
        map.put(PlayerSheet.KEY_NOTES, repeat('x', 5000));
        map.put(PlayerSheet.KEY_AC, "  18  ");
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(PlayerSheet.MAX_NAME, s.name.length());
        assertEquals(PlayerSheet.MAX_NOTES, s.notes.length());
        assertEquals("18", s.ac);
    }

    @Test
    public void cuttingDoesNotSplitAnEmoji() {
        PlayerSheet s = new PlayerSheet();
        // A dragon emoji (two chars) right across the limit
        s.name = repeat('a', PlayerSheet.MAX_NAME - 1) + "🐉";
        String cut = s.cleaned().name;
        assertEquals(PlayerSheet.MAX_NAME - 1, cut.length());
        assertFalse(Character.isHighSurrogate(cut.charAt(cut.length() - 1)));
    }

    @Test
    public void wrongTypesCountAsMissing() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_HP, "lots");
        map.put(PlayerSheet.KEY_SAVES, 12L);
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(0, s.hp);
        assertEquals("", s.saves);
    }

    @Test
    public void noNameOrNoMapGivesNull() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_NAME, "   ");
        assertNull(PlayerSheet.fromMap(map));
        assertNull(PlayerSheet.fromMap(null));
        assertNull(PlayerSheet.fromMap(new HashMap<>()));
    }

    @Test
    public void applyToKeepsDebuffsAndTurnOrder() {
        CharacterModel c = new CharacterModel();
        c.getDebuffList().add(new DebuffModel());
        c.setFirstInRound(true);
        c.setFastHealing(2);
        valeros().applyTo(c);
        assertEquals("Valeros", c.getCharacterName());
        assertEquals(30, c.getHp());
        assertEquals("Power attack", c.getCharacterNotes());
        assertTrue(c.isPC());
        assertTrue(c.isFirstInRound());
        assertEquals(1, c.getDebuffList().size());
        assertEquals(2, c.getFastHealing());
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
