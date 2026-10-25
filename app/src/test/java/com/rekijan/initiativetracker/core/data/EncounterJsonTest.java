package com.rekijan.initiativetracker.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

import java.util.List;

/**
 * Tests {@link EncounterJson}: saves made by 1.x must load in 2.0, and what 2.0 saves must load
 * back the same.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class EncounterJsonTest {

    /** A list as 1.35 saved it under GSON_TAG: includes the old id fields, one without debuffList. */
    private static final String SAVE_FROM_1_X = "["
            + "{\"id\":0,\"party_id\":0,\"initiative\":18,\"initiativeBonus\":4,\"skills\":\"Stealth +9\","
            + "\"attackRoutine\":\"Rapier +7 (1d6+2)\",\"ac\":\"AC 17 (touch 14, \\n flatfooted 13)\","
            + "\"saves\":\"Fort 3 Ref 8 \\n Will 2\",\"maneuvers\":\"CMB +3 CMD 17\",\"hp\":21,\"maxHp\":27,"
            + "\"isFirstInRound\":true,\"isPC\":true,\"fastHealing\":0,\"regeneration\":0,"
            + "\"characterName\":\"Merisiel\",\"characterNotes\":\"Has the key\","
            + "\"debuffList\":[{\"name\":\"Haste\",\"duration\":3,\"description\":\"+1 attack\"}]},"
            + "{\"id\":0,\"party_id\":0,\"initiative\":9,\"initiativeBonus\":1,\"hp\":-2,\"maxHp\":15,"
            + "\"isFirstInRound\":false,\"isPC\":false,\"characterName\":\"Goblin\"}"
            + "]";

    @Test
    public void loadsSaveFrom1x() {
        List<CharacterModel> characters = EncounterJson.fromJson(SAVE_FROM_1_X);

        assertEquals(2, characters.size());
        CharacterModel pc = characters.get(0);
        assertEquals("Merisiel", pc.getCharacterName());
        assertEquals(18, pc.getInitiative());
        assertEquals(4, pc.getInitiativeBonus());
        assertEquals(21, pc.getHp());
        assertEquals(27, pc.getMaxHp());
        assertTrue(pc.isPC());
        assertTrue(pc.isFirstInRound());
        assertEquals("Stealth +9", pc.getSkills());
        assertEquals("Has the key", pc.getCharacterNotes());
        assertEquals(1, pc.getDebuffList().size());
        DebuffModel haste = pc.getDebuffList().get(0);
        assertEquals("Haste", haste.getName());
        assertEquals(3, haste.getDuration());
        assertEquals("+1 attack", haste.getDescription());

        CharacterModel npc = characters.get(1);
        assertFalse(npc.isPC());
        assertEquals(-2, npc.getHp());
        assertEquals("", npc.getSkills());
        assertTrue(npc.getDebuffList().isEmpty());
    }

    @Test
    public void roundTripKeepsEverything() {
        List<CharacterModel> original = EncounterJson.fromJson(SAVE_FROM_1_X);
        List<CharacterModel> reloaded = EncounterJson.fromJson(EncounterJson.toJson(original));

        assertEquals(original.size(), reloaded.size());
        assertEquals("Merisiel", reloaded.get(0).getCharacterName());
        assertEquals(3, reloaded.get(0).getDebuffList().get(0).getDuration());
        assertEquals(-2, reloaded.get(1).getHp());
    }

    @Test
    public void savesUseThe1xFieldNames() {
        String json = EncounterJson.toJson(EncounterJson.fromJson(SAVE_FROM_1_X));
        assertTrue(json.contains("\"isFirstInRound\":true"));
        assertTrue(json.contains("\"isPC\":true"));
        assertTrue(json.contains("\"debuffList\":["));
        assertTrue(json.contains("\"characterName\":\"Merisiel\""));
    }

    @Test
    public void nothingOrBrokenSaveGivesNull() {
        assertNull(EncounterJson.fromJson(null));
        assertNull(EncounterJson.fromJson(""));
        assertNull(EncounterJson.fromJson("{not json"));
    }
}
