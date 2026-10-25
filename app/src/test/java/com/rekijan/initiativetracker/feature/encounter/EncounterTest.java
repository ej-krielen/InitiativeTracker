package com.rekijan.initiativetracker.feature.encounter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the turn order rules of {@link Encounter}: sorting, next turn and round counting,
 * removing, manual reordering and healing PCs.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class EncounterTest {

    private static CharacterModel character(String name, int initiative) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        c.setInitiative(initiative);
        return c;
    }

    private static Encounter encounterOf(CharacterModel... characters) {
        return new Encounter(new ArrayList<>(Arrays.asList(characters)), 1);
    }

    private static List<String> names(Encounter encounter) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) names.add(c.getCharacterName());
        return names;
    }

    @Test
    public void sortOrdersHighToLowAndMarksTopAsFirstInRound() {
        Encounter e = encounterOf(character("A", 5), character("B", 20), character("C", 12));
        boolean duplicate = e.sortByInitiative();

        assertFalse(duplicate);
        assertEquals(Arrays.asList("B", "C", "A"), names(e));
        assertTrue(e.get(0).isFirstInRound());
        assertFalse(e.get(1).isFirstInRound());
        assertFalse(e.get(2).isFirstInRound());
    }

    @Test
    public void replacePutsInAnotherPartyAndTellsListeners() {
        Encounter e = encounterOf(character("A", 5));
        int[] calls = {0};
        e.addListener(() -> calls[0]++);

        e.replace(Arrays.asList(character("X", 1), character("Y", 2)), 6);

        assertEquals(Arrays.asList("X", "Y"), names(e));
        assertEquals(6, e.getRound());
        assertEquals(1, calls[0]);
    }

    @Test
    public void sortReportsDuplicateInitiative() {
        Encounter e = encounterOf(character("A", 10), character("B", 10), character("C", 3));
        assertTrue(e.sortByInitiative());
    }

    @Test
    public void sortAndNextTurnOnEmptyEncounterDoNotCrash() {
        Encounter e = encounterOf();
        assertFalse(e.sortByInitiative());
        assertNull(e.nextTurn());
    }

    @Test
    public void nextTurnRotatesAndCountsRoundWhenFirstInRoundComesUp() {
        Encounter e = encounterOf(character("A", 20), character("B", 10), character("C", 5));
        e.sortByInitiative();
        e.resetRound();

        Encounter.Turn turn = e.nextTurn();
        assertEquals("B", turn.active.getCharacterName());
        assertFalse(turn.newRound);
        assertEquals(1, e.getRound());

        e.nextTurn(); // C
        turn = e.nextTurn(); // back to A
        assertEquals("A", turn.active.getCharacterName());
        assertTrue(turn.newRound);
        assertEquals(2, e.getRound());
        assertEquals(Arrays.asList("A", "B", "C"), names(e));
    }

    @Test
    public void nextTurnTicksDebuffsOfActiveCharacterOnly() {
        CharacterModel a = character("A", 20);
        CharacterModel b = character("B", 10);
        DebuffModel shaken = new DebuffModel("Shaken", 1, "");
        DebuffModel haste = new DebuffModel("Haste", 3, "");
        b.getDebuffList().add(shaken);
        b.getDebuffList().add(haste);
        a.getDebuffList().add(new DebuffModel("Bless", 2, ""));
        Encounter e = encounterOf(a, b);

        Encounter.Turn turn = e.nextTurn();

        assertSame(b, turn.active);
        assertEquals(1, turn.expiredDebuffs.size());
        assertSame(shaken, turn.expiredDebuffs.get(0));
        assertEquals(0, shaken.getDuration());
        assertEquals(2, haste.getDuration());
        assertEquals(2, a.getDebuffList().get(0).getDuration());
    }

    @Test
    public void removingFirstInRoundPassesItToTheNextInOrder() {
        Encounter e = encounterOf(character("A", 20), character("B", 10), character("C", 5));
        e.sortByInitiative(); // A first in round
        e.nextTurn();         // B, C, A
        e.nextTurn();         // C, A, B -> A is at position 1
        e.remove(1);          // remove A

        // B acts after C, so B now starts the next round
        assertEquals(Arrays.asList("C", "B"), names(e));
        assertTrue(e.get(1).isFirstInRound());
        assertFalse(e.get(0).isFirstInRound());

        int round = e.getRound();
        Encounter.Turn turn = e.nextTurn();
        assertEquals("B", turn.active.getCharacterName());
        assertTrue(turn.newRound);
        assertEquals(round + 1, e.getRound());
    }

    @Test
    public void removingLastCharacterInListWrapsFirstInRoundToTop() {
        Encounter e = encounterOf(character("A", 20), character("B", 10), character("C", 5));
        e.setFirstInRound(2);
        e.remove(2);
        assertTrue(e.get(0).isFirstInRound());
    }

    @Test
    public void removeOutOfRangeIsIgnored() {
        Encounter e = encounterOf(character("A", 1));
        e.remove(5);
        e.remove(-1);
        assertEquals(1, e.size());
    }

    @Test
    public void moveUpAndDownSwapAndWrapAround() {
        Encounter e = encounterOf(character("A", 0), character("B", 0), character("C", 0));
        e.moveUp(1);
        assertEquals(Arrays.asList("B", "A", "C"), names(e));
        e.moveUp(0);
        assertEquals(Arrays.asList("C", "A", "B"), names(e));
        e.moveDown(2);
        assertEquals(Arrays.asList("B", "A", "C"), names(e));
        e.moveDown(0);
        assertEquals(Arrays.asList("A", "B", "C"), names(e));
    }

    @Test
    public void setFirstInRoundMarksOnlyOne() {
        Encounter e = encounterOf(character("A", 0), character("B", 0), character("C", 0));
        e.setFirstInRound(0);
        e.setFirstInRound(2);
        assertFalse(e.get(0).isFirstInRound());
        assertTrue(e.get(2).isFirstInRound());
    }

    @Test
    public void setPcsToMaxHpSkipsNpcsAndPcsWithTemporaryHp() {
        CharacterModel hurt = character("Hurt", 0);
        hurt.setPC(true);
        hurt.setMaxHp(30);
        hurt.setHp(10);
        CharacterModel tempHp = character("Temp", 0);
        tempHp.setPC(true);
        tempHp.setMaxHp(30);
        tempHp.setHp(35);
        CharacterModel npc = character("Orc", 0);
        npc.setMaxHp(15);
        npc.setHp(2);
        Encounter e = encounterOf(hurt, tempHp, npc);

        List<String> skipped = e.setPcsToMaxHp();

        assertEquals(30, hurt.getHp());
        assertEquals(35, tempHp.getHp());
        assertEquals(2, npc.getHp());
        assertEquals(List.of("Temp"), skipped);
    }

    @Test
    public void roundCounterNeverGoesBelowZero() {
        Encounter e = new Encounter(new ArrayList<>(), 1);
        e.previousRound();
        e.previousRound();
        assertEquals(0, e.getRound());
        e.nextRound();
        assertEquals(1, e.getRound());
        assertEquals(0, new Encounter(new ArrayList<>(), -4).getRound());
    }

    @Test
    public void listenersAreToldAboutChanges() {
        Encounter e = encounterOf(character("A", 0));
        int[] calls = {0};
        Encounter.Listener listener = () -> calls[0]++;
        e.addListener(listener);
        e.nextRound();
        e.add(character("B", 0));
        e.removeListener(listener);
        e.nextRound();
        assertEquals(2, calls[0]);
    }
}
