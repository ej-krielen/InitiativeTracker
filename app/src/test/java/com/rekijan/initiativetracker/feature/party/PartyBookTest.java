package com.rekijan.initiativetracker.feature.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link PartyBook}: creating, renaming, duplicating, deleting and switching parties, the
 * party limit, and that every party keeps its own characters and round counter.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartyBookTest {

    private MemoryKeyValueStore values;
    private PartyBook book;

    @Before
    public void setUp() {
        values = new MemoryKeyValueStore();
        book = new PartyBook(new PartyStore(values, "My party"), "My party");
    }

    private static List<CharacterModel> characters(String... names) {
        List<CharacterModel> list = new ArrayList<>();
        for (String name : names) {
            CharacterModel c = new CharacterModel();
            c.setCharacterName(name);
            list.add(c);
        }
        return list;
    }

    private PartyBook reload() {
        return new PartyBook(new PartyStore(values, "My party"), "My party");
    }

    @Test
    public void eachPartyKeepsItsOwnCharactersAndRound() {
        String first = book.getActiveId();
        book.save(first, characters("Merisiel", "Goblin"), 4);
        Party second = book.create("Curse of Strahd", characters("Ireena"));
        book.setActive(second.getId());
        book.save(second.getId(), characters("Ireena", "Strahd"), 9);

        PartyBook again = reload();
        assertEquals(second.getId(), again.getActiveId());
        assertEquals(2, again.loadCharacters(first).size());
        assertEquals("Merisiel", again.loadCharacters(first).get(0).getCharacterName());
        assertEquals(4, again.loadRound(first));
        assertEquals("Strahd", again.loadCharacters(second.getId()).get(1).getCharacterName());
        assertEquals(9, again.loadRound(second.getId()));
    }

    @Test
    public void newPartyStartsAtRoundZeroAndIsNotActive() {
        Party party = book.create("  One-shot ", characters("A", "B", "C"));
        assertEquals("One-shot", party.getName());
        assertEquals(0, book.loadRound(party.getId()));
        assertEquals(3, book.characterCount(party.getId()));
        assertNotEquals(party.getId(), book.getActiveId());
    }

    @Test
    public void createRefusesBadNames() {
        assertNull(book.create("", characters()));
        assertNull(book.create("my PARTY", characters()));
        assertNull(book.create("x".repeat(PartyNames.MAX_LENGTH + 1), characters()));
        assertEquals(1, book.getUsed());
    }

    @Test
    public void fivePartiesAreFree() {
        for (int i = 2; i <= 5; i++) assertNotNull(book.create("P" + i, characters()));
        assertEquals(5, book.getUsed());
        assertFalse(book.canCreate());
        assertNull(book.create("P6", characters()));
        assertNull(book.duplicate(book.getActiveId()));
    }

    @Test
    public void packAddsFiveAndRefundKeepsParties() {
        for (int i = 2; i <= 5; i++) book.create("P" + i, characters());
        book.setPackLevel(1);
        assertEquals(10, book.getLimit());
        assertNotNull(book.create("P6", characters()));
        assertEquals(1, reload().getPackLevel());

        // Refund: all six stay, only new ones are blocked
        book.setPackLevel(0);
        assertEquals(6, reload().getUsed());
        assertFalse(book.canCreate());
    }

    @Test
    public void renameChecksTheName() {
        Party other = book.create("Other", characters());
        assertTrue(book.rename(other.getId(), " Thursday group "));
        assertEquals("Thursday group", reload().get(other.getId()).getName());
        assertFalse(book.rename(other.getId(), "my party"));
        assertFalse(book.rename(other.getId(), " "));
        assertFalse(book.rename("unknown", "Name"));
    }

    @Test
    public void duplicateCopiesCharactersAndRound() {
        String first = book.getActiveId();
        book.save(first, characters("Merisiel"), 3);
        Party copy = book.duplicate(first);

        assertEquals("My party 2", copy.getName());
        assertEquals(1, book.getParties().indexOf(copy));
        assertEquals("Merisiel", book.loadCharacters(copy.getId()).get(0).getCharacterName());
        assertEquals(3, book.loadRound(copy.getId()));

        // A copy is separate: changing it leaves the original alone
        book.save(copy.getId(), characters("Kyra", "Valeros"), 1);
        assertEquals(1, book.characterCount(first));
        assertEquals(3, book.loadRound(first));
    }

    @Test
    public void lastPartyCannotBeDeleted() {
        assertNull(book.delete(book.getActiveId()));
        assertEquals(1, book.getUsed());
    }

    @Test
    public void deletingTheActivePartyActivatesTheOneAbove() {
        String first = book.getActiveId();
        Party second = book.create("Second", characters("A"));
        Party third = book.create("Third", characters("B"));
        book.setActive(third.getId());

        assertEquals(second.getId(), book.delete(third.getId()));
        assertNull(book.loadCharacters(third.getId()));
        assertEquals(first, book.delete(second.getId()));
        assertEquals(first, reload().getActiveId());
    }

    @Test
    public void deletingAnotherPartyKeepsTheActiveOne() {
        Party second = book.create("Second", characters());
        String first = book.getActiveId();
        assertEquals(first, book.delete(second.getId()));
        assertEquals(1, book.getUsed());
    }

    @Test
    public void unknownActiveIdFallsBackToTheFirstParty() {
        Party second = book.create("Second", characters());
        book.setActive("gone");
        assertNotEquals("gone", book.getActiveId());
        book.setActive(second.getId());
        assertEquals(second.getId(), reload().getActiveId());
    }
}
