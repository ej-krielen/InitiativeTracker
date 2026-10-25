package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests {@link GmCopy}: the GM's hp, max hp and conditions survive the trip to Firestore and
 * back, replace those on the player's sheet without touching the rest, and broken or oversized
 * documents are cleaned up.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class GmCopyTest {

    private static CharacterModel gmValeros() {
        CharacterModel c = new CharacterModel();
        c.setCharacterName("Valeros");
        c.setHp(12);
        c.setMaxHp(34);
        c.getDebuffList().add(new DebuffModel("Shaken", 2, "-2 attacks"));
        c.getDebuffList().add(new DebuffModel("Haste", 3, "+1 attack"));
        return c;
    }

    @Test
    public void roundTripKeepsHpAndConditions() {
        GmCopy back = GmCopy.fromMap(GmCopy.of(gmValeros()).toMap());
        assertEquals(12, back.hp);
        assertEquals(34, back.maxHp);
        assertEquals(2, back.conditions.size());
        assertEquals("Shaken", back.conditions.get(0).getName());
        assertEquals(2, back.conditions.get(0).getDuration());
        assertEquals("+1 attack", back.conditions.get(1).getDescription());
    }

    @Test
    public void applyReplacesOnlyHpAndConditions() {
        CharacterModel player = new CharacterModel();
        player.setCharacterName("Valeros");
        player.setHp(34);
        player.setMaxHp(30);
        player.setAc("18");
        player.setInitiative(15);
        player.getDebuffList().add(new DebuffModel("Bless", 5, ""));

        GmCopy.of(gmValeros()).applyTo(player);

        assertEquals(12, player.getHp());
        assertEquals(34, player.getMaxHp());
        assertEquals(2, player.getDebuffList().size());
        assertEquals("Shaken", player.getDebuffList().get(0).getName());
        assertEquals("18", player.getAc());
        assertEquals(15, player.getInitiative());
        assertEquals("Valeros", player.getCharacterName());
    }

    @Test
    public void tooManyConditionsAreCut() {
        CharacterModel c = gmValeros();
        for (int i = 0; i < 80; i++) c.getDebuffList().add(new DebuffModel("x" + i, 1, ""));
        assertEquals(GmCopy.MAX_CONDITIONS, GmCopy.of(c).conditions.size());
    }

    @Test
    public void brokenDocumentIsCleaned() {
        Map<String, Object> map = new HashMap<>();
        map.put(GmCopy.KEY_HP, 1e12);
        map.put(GmCopy.KEY_MAX_HP, "lots");
        List<Object> conditions = new ArrayList<>();
        conditions.add("not a map");
        Map<String, Object> long1 = new HashMap<>();
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 300; i++) name.append('n');
        long1.put(GmCopy.KEY_CONDITION_NAME, name.toString());
        long1.put(GmCopy.KEY_CONDITION_DURATION, -5L);
        conditions.add(long1);
        map.put(GmCopy.KEY_CONDITIONS, conditions);

        GmCopy copy = GmCopy.fromMap(map);
        assertEquals(CharacterModel.MAX_HP, copy.hp);
        assertEquals(0, copy.maxHp);
        assertEquals(1, copy.conditions.size());
        assertEquals(GmCopy.MAX_CONDITION_NAME, copy.conditions.get(0).getName().length());
        assertEquals(0, copy.conditions.get(0).getDuration());
        assertEquals("", copy.conditions.get(0).getDescription());
    }

    @Test
    public void noMapGivesNull() {
        assertNull(GmCopy.fromMap(null));
    }

    @Test
    public void numbersAreSentAsLong() {
        Map<String, Object> map = GmCopy.of(gmValeros()).toMap();
        assertTrue(map.get(GmCopy.KEY_HP) instanceof Long);
        assertTrue(map.get(GmCopy.KEY_CONDITIONS) instanceof List);
    }
}
