package com.rekijan.initiativetracker.feature.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import org.junit.Test;

import java.util.List;

/**
 * Tests {@link PartyStore}, mainly the 1.x migration: a 1.x save must become the first party
 * with all its characters and its round counter, and nothing of the old save may be lost.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartyStoreTest {

    /** A fight as 1.35 saved it under GSON_TAG, with the old id fields. */
    private static final String SAVE_FROM_1_X = "["
            + "{\"id\":0,\"party_id\":0,\"initiative\":18,\"initiativeBonus\":4,\"hp\":21,\"maxHp\":27,"
            + "\"isFirstInRound\":false,\"isPC\":true,\"characterName\":\"Merisiel\","
            + "\"debuffList\":[{\"name\":\"Haste\",\"duration\":3,\"description\":\"+1 attack\"}]},"
            + "{\"id\":0,\"party_id\":0,\"initiative\":9,\"initiativeBonus\":1,\"hp\":-2,\"maxHp\":15,"
            + "\"isFirstInRound\":true,\"isPC\":false,\"characterName\":\"Goblin\"}"
            + "]";

    private static MemoryKeyValueStore saveFrom1x() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit()
                .putString(PartyStore.KEY_1X_CHARACTERS, SAVE_FROM_1_X)
                .putInt(PartyStore.KEY_1X_ROUND, 7)
                .apply();
        return values;
    }

    @Test
    public void saveFrom1xBecomesTheFirstParty() {
        MemoryKeyValueStore values = saveFrom1x();
        PartyBook book = new PartyBook(new PartyStore(values, "My party"), "unused");

        assertEquals(1, book.getParties().size());
        Party first = book.getActive();
        assertEquals("My party", first.getName());
        assertEquals(7, book.loadRound(first.getId()));

        List<CharacterModel> characters = book.loadCharacters(first.getId());
        assertEquals(2, characters.size());
        assertEquals("Merisiel", characters.get(0).getCharacterName());
        assertEquals(21, characters.get(0).getHp());
        assertEquals("Haste", characters.get(0).getDebuffList().get(0).getName());
        assertEquals("Goblin", characters.get(1).getCharacterName());
        assertTrue(characters.get(1).isFirstInRound());
    }

    @Test
    public void oldKeysAreKeptAsSafetyNet() {
        MemoryKeyValueStore values = saveFrom1x();
        new PartyStore(values, "My party");

        assertEquals(SAVE_FROM_1_X, values.getString(PartyStore.KEY_1X_CHARACTERS, null));
        assertEquals(7, values.getInt(PartyStore.KEY_1X_ROUND, 0));
    }

    @Test
    public void migrationRunsOnce() {
        MemoryKeyValueStore values = saveFrom1x();
        PartyStore store = new PartyStore(values, "My party");
        String id = store.loadActiveId();
        store.saveFight(id, "[]", 2);

        // Next start: the party is not overwritten by the 1.x save again
        PartyStore again = new PartyStore(values, "My party");
        assertEquals(1, again.loadParties().size());
        assertEquals(id, again.loadActiveId());
        assertEquals("[]", again.loadCharactersJson(id));
        assertEquals(2, again.loadRound(id));
    }

    @Test
    public void unreadable1xSaveIsStillCopied() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit().putString(PartyStore.KEY_1X_CHARACTERS, "{not json").apply();
        PartyStore store = new PartyStore(values, "My party");

        assertEquals("{not json", store.loadCharactersJson(store.loadActiveId()));
    }

    @Test
    public void freshInstallGetsOneEmptyParty() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        PartyBook book = new PartyBook(new PartyStore(values, "My party"), "unused");

        assertEquals(1, book.getParties().size());
        assertNull(book.loadCharacters(book.getActiveId()));
        assertEquals(0, book.loadRound(book.getActiveId()));
    }

    @Test
    public void packLevelIsSavedAndClamped() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        PartyStore store = new PartyStore(values, "My party");
        store.savePackLevel(2);
        assertEquals(2, new PartyStore(values, "My party").loadPackLevel());
        store.savePackLevel(12);
        assertEquals(PartySlots.MAX_PACK_LEVEL, store.loadPackLevel());
    }
}
