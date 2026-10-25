package com.rekijan.initiativetracker.feature.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

/**
 * Tests {@link PlayerBook}: player characters are made as PCs, saved and read back with their
 * conditions, renamed, deleted, and there is no limit.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerBookTest {

    private final MemoryKeyValueStore values = new MemoryKeyValueStore();
    private final PlayerBook book = new PlayerBook(values);

    @Test
    public void createMakesANamedPc() {
        String id = book.create("  Valeros ");
        assertNotNull(id);
        CharacterModel c = book.load(id);
        assertEquals("Valeros", c.getCharacterName());
        assertTrue(c.isPC());
        assertEquals("Valeros", book.getName(id));
    }

    @Test
    public void emptyNameIsRefused() {
        assertNull(book.create("   "));
        assertTrue(book.getIds().isEmpty());
    }

    @Test
    public void savedCharacterSurvivesAReopen() {
        String id = book.create("Seoni");
        CharacterModel c = book.load(id);
        c.setHp(21);
        c.getDebuffList().add(new DebuffModel("Mage armor", 60, "+4 AC"));
        book.save(id, c);

        PlayerBook reopened = new PlayerBook(values);
        assertEquals(1, reopened.getIds().size());
        CharacterModel back = reopened.load(id);
        assertEquals(21, back.getHp());
        assertEquals("Mage armor", back.getDebuffList().get(0).getName());
    }

    @Test
    public void renameChangesTheCharacterName() {
        String id = book.create("Seoni");
        assertTrue(book.rename(id, "Seelah"));
        assertEquals("Seelah", book.load(id).getCharacterName());
        assertFalse(book.rename(id, ""));
        assertFalse(book.rename("unknown", "X"));
    }

    @Test
    public void deleteRemovesItsData() {
        String id = book.create("Seoni");
        int before = values.size();
        book.delete(id);
        assertFalse(book.contains(id));
        assertTrue(values.size() < before);
        // Saving a deleted character does nothing
        book.save(id, new CharacterModel());
        assertFalse(new PlayerBook(values).contains(id));
    }

    @Test
    public void thereIsNoLimit() {
        for (int i = 0; i < 30; i++) assertNotNull(book.create("PC " + i));
        assertEquals(30, book.getIds().size());
    }

    @Test
    public void unreadableListStartsEmpty() {
        values.edit().putString(PlayerBook.KEY_IDS, "{broken").apply();
        assertTrue(new PlayerBook(values).getIds().isEmpty());
    }
}
