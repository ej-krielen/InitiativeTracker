package com.rekijan.initiativetracker.feature.spell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.EncounterJson;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.sync.PlayerSheet;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests spells: casting on several targets, counting down on the caster's turn, ending early,
 * a caster leaving the fight (spell cards), saving and loading, old saves and Table sync.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2026
 */
public class SpellsTest {

    private static CharacterModel character(String name, int initiative) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        c.setInitiative(initiative);
        return c;
    }

    /** Mira (caster, active and first in round), Rekijan, Tor, Ash, in that order. */
    private CharacterModel mira, rekijan, tor, ash;
    private Encounter encounter;

    private void setUpParty() {
        mira = character("Mira", 20);
        rekijan = character("Rekijan", 15);
        tor = character("Tor", 12);
        ash = character("Ash", 8);
        mira.setFirstInRound(true);
        encounter = new Encounter(new ArrayList<>(Arrays.asList(mira, rekijan, tor, ash)), 1);
    }

    private static DebuffModel only(CharacterModel c) {
        assertEquals(1, c.getDebuffList().size());
        return c.getDebuffList().get(0);
    }

    private static List<String> names(Encounter e) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : e.getCharacters()) names.add(c.getCharacterName());
        return names;
    }

    @Test
    public void castPutsOneConditionOnEveryTarget() {
        setUpParty();
        String spell = Spells.cast(encounter, mira, Arrays.asList(rekijan, tor, ash), "  Bless ", 3, "+1 attack");

        assertNotNull(spell);
        for (CharacterModel target : Arrays.asList(rekijan, tor, ash)) {
            DebuffModel d = only(target);
            assertEquals("Bless", d.getName());
            assertEquals(3, d.getDuration());
            assertEquals(mira.getStableId(), d.getCasterId());
            assertEquals("Mira", d.getCasterName());
            assertEquals(spell, d.getSpellId());
        }
        assertTrue(mira.getDebuffList().isEmpty());
        assertEquals(Arrays.asList(rekijan, tor, ash), Spells.targetsOf(encounter, spell));
    }

    @Test
    public void castWithoutTargetsDoesNothing() {
        setUpParty();
        assertNull(Spells.cast(encounter, mira, Collections.emptyList(), "Bless", 3, ""));
    }

    @Test
    public void durationFieldIsEmptyOrOneTo9999() {
        assertEquals(0, Spells.parseDuration(""));
        assertEquals(0, Spells.parseDuration("  "));
        assertEquals(1, Spells.parseDuration("1"));
        assertEquals(9999, Spells.parseDuration("9999"));
        assertEquals(-1, Spells.parseDuration("0"));
        assertEquals(-1, Spells.parseDuration("10000"));
        assertEquals(-1, Spells.parseDuration("-3"));
        assertEquals(-1, Spells.parseDuration("3x"));
        assertEquals(-1, Spells.parseDuration("99999999999"));
    }

    @Test
    public void spellCardsCanNotCastOrBeTargeted() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(tor), "Bless", 2, "");
        encounter.remove(mira);
        assertEquals(Arrays.asList(rekijan, tor, ash), Spells.combatants(encounter));
    }

    @Test
    public void castClampsTheDuration() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(tor), "Bless", 99999, "");
        assertEquals(DebuffModel.MAX_DURATION, only(tor).getDuration());
    }

    @Test
    public void spellCountsDownOnTheCastersTurnOnly() {
        setUpParty();
        Spells.cast(encounter, mira, Arrays.asList(rekijan, tor, ash), "Bless", 2, "");

        // Rekijan, Tor and Ash take their turns: nothing counts down
        for (int i = 0; i < 3; i++) {
            Encounter.Turn turn = encounter.nextTurn();
            assertTrue(turn.ticks.isEmpty());
            assertEquals(2, only(rekijan).getDuration());
        }

        // Mira's turn: every target goes down, reported per target
        Encounter.Turn turn = encounter.nextTurn();
        assertSame(mira, turn.active);
        assertEquals(3, turn.ticks.size());
        assertSame(rekijan, turn.ticks.get(0).target);
        assertEquals(2, turn.ticks.get(0).from);
        assertEquals(1, turn.ticks.get(0).to);
        assertTrue(turn.expired.isEmpty());
        assertEquals(1, only(ash).getDuration());

        // Round two: ends on Mira's turn, on all three
        for (int i = 0; i < 3; i++) encounter.nextTurn();
        turn = encounter.nextTurn();
        assertSame(mira, turn.active);
        assertEquals(3, turn.expired.size());
        assertSame(rekijan, turn.expired.get(0).character);
        assertSame(tor, turn.expired.get(1).character);
        assertSame(ash, turn.expired.get(2).character);
        assertEquals(0, only(tor).getDuration());
        assertEquals(3, encounter.getRound());
    }

    @Test
    public void ownConditionsStillCountOnTheirOwnTurn() {
        setUpParty();
        rekijan.getDebuffList().add(new DebuffModel("Prone", 2, ""));
        Spells.cast(encounter, mira, Collections.singletonList(rekijan), "Bless", 5, "");

        Encounter.Turn turn = encounter.nextTurn();

        assertSame(rekijan, turn.active);
        assertEquals(1, turn.ticks.size());
        assertEquals("Prone", turn.ticks.get(0).name);
        assertNull(turn.ticks.get(0).target);
        assertEquals(5, rekijan.getDebuffList().get(1).getDuration());
    }

    @Test
    public void casterCanTargetThemselves() {
        setUpParty();
        Spells.cast(encounter, rekijan, Arrays.asList(rekijan, tor), "Shield", 2, "");

        Encounter.Turn turn = encounter.nextTurn();

        assertSame(rekijan, turn.active);
        assertEquals(2, turn.ticks.size());
        assertNull(turn.ticks.get(0).target);
        assertSame(tor, turn.ticks.get(1).target);
        assertEquals(1, only(rekijan).getDuration());
        assertEquals(1, only(tor).getDuration());
    }

    @Test
    public void endTakesTheSpellOffEveryTarget() {
        setUpParty();
        tor.getDebuffList().add(new DebuffModel("Prone", 0, ""));
        String bless = Spells.cast(encounter, mira, Arrays.asList(rekijan, tor), "Bless", 3, "");
        Spells.cast(encounter, mira, Collections.singletonList(tor), "Haste", 3, "");

        Spells.end(encounter, bless);

        assertTrue(rekijan.getDebuffList().isEmpty());
        assertEquals(2, tor.getDebuffList().size());
        assertEquals("Prone", tor.getDebuffList().get(0).getName());
        assertEquals("Haste", tor.getDebuffList().get(1).getName());
    }

    @Test
    public void removedCasterIsReplacedBySpellCardsOnTheSameSpot() {
        setUpParty();
        encounter.nextTurn(); // Rekijan's turn; Mira at the bottom
        String bless = Spells.cast(encounter, mira, Arrays.asList(rekijan, tor), "Bless", 2, "");
        String haste = Spells.cast(encounter, mira, Collections.singletonList(ash), "Haste", 3, "");

        encounter.remove(mira);

        assertEquals(Arrays.asList("Rekijan", "Tor", "Ash", "Bless", "Haste"), names(encounter));
        CharacterModel blessCard = encounter.get(3);
        CharacterModel hasteCard = encounter.get(4);
        assertTrue(blessCard.isSpellCard());
        assertEquals("Mira", blessCard.getSpellCardCaster());
        assertEquals(20, blessCard.getInitiative());
        // Mira was first in round: the first card takes that over
        assertTrue(blessCard.isFirstInRound());
        assertFalse(hasteCard.isFirstInRound());
        assertEquals(blessCard.getStableId(), only(rekijan).getCasterId());
        assertEquals(hasteCard.getStableId(), only(ash).getCasterId());
        assertEquals(bless, Spells.spellOfCard(encounter, blessCard));
        assertEquals(haste, Spells.spellOfCard(encounter, hasteCard));

        List<Encounter.CasterGone> gone = encounter.takeCastersGone();
        assertEquals(1, gone.size());
        assertEquals("Mira", gone.get(0).casterName);
        assertEquals(Arrays.asList(blessCard, hasteCard), gone.get(0).spellCards);
        assertTrue(encounter.takeCastersGone().isEmpty());

        // The spell keeps counting on the card's turn, on the old initiative
        encounter.nextTurn(); // Tor
        encounter.nextTurn(); // Ash
        Encounter.Turn turn = encounter.nextTurn(); // Bless card
        assertSame(blessCard, turn.active);
        assertTrue(turn.newRound);
        assertEquals(1, only(rekijan).getDuration());
        assertEquals(3, only(ash).getDuration());
    }

    @Test
    public void casterWithoutSpellsOnOthersJustLeaves() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(mira), "Shield", 2, "");

        encounter.remove(mira);

        assertEquals(Arrays.asList("Rekijan", "Tor", "Ash"), names(encounter));
        assertTrue(encounter.takeCastersGone().isEmpty());
        assertTrue(rekijan.isFirstInRound());
    }

    @Test
    public void casterRemovedWithAllTargetsGetsNoCards() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(tor), "Bless", 2, "");

        encounter.removeAll(Arrays.asList(mira, tor));

        assertEquals(Arrays.asList("Rekijan", "Ash"), names(encounter));
        assertTrue(encounter.takeCastersGone().isEmpty());
    }

    @Test
    public void endingTheSpellOfACardRemovesTheCard() {
        setUpParty();
        String bless = Spells.cast(encounter, mira, Arrays.asList(rekijan, tor), "Bless", 2, "");
        encounter.nextTurn(); // Rekijan's turn
        encounter.remove(mira);
        assertEquals(4, encounter.size());

        Spells.end(encounter, bless);

        assertEquals(Arrays.asList("Rekijan", "Tor", "Ash"), names(encounter));
        assertTrue(rekijan.getDebuffList().isEmpty());
        // The card was first in round; that went to the next in line
        assertTrue(rekijan.isFirstInRound());
    }

    @Test
    public void removingASpellCardEndsItsSpell() {
        setUpParty();
        Spells.cast(encounter, mira, Arrays.asList(rekijan, tor), "Bless", 2, "");
        encounter.remove(mira);
        CharacterModel card = encounter.get(0);

        encounter.remove(card);

        assertTrue(rekijan.getDebuffList().isEmpty());
        assertTrue(tor.getDebuffList().isEmpty());
        assertEquals(3, encounter.size());
    }

    @Test
    public void spellCardWhoseSpellExpiredLeavesWhenTheTurnMovesOn() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(tor), "Bless", 1, "");
        encounter.nextTurn(); // Rekijan
        encounter.remove(mira);
        encounter.nextTurn(); // Tor
        encounter.nextTurn(); // Ash
        Encounter.Turn turn = encounter.nextTurn(); // the Bless card: it ends
        CharacterModel card = turn.active;
        assertTrue(card.isSpellCard());
        assertEquals(1, turn.expired.size());

        // The GM removes the ended condition; the card stays while it is having its turn
        tor.getDebuffList().clear();
        encounter.notifyChanged();
        assertSame(card, encounter.get(0));

        turn = encounter.nextTurn();
        assertSame(rekijan, turn.active);
        assertFalse(encounter.getCharacters().contains(card));
        assertEquals(3, encounter.size());
        // The round started on the card's turn, so Rekijan doesn't start another
        assertFalse(turn.newRound);
        assertEquals(2, encounter.getRound());
    }

    @Test
    public void conditionWhoseCasterIsMissingCountsOnItsOwnTurn() {
        setUpParty();
        DebuffModel d = new DebuffModel("Bless", 2, "");
        d.setCaster("someone-not-here", "Ghost", "spell");
        rekijan.getDebuffList().add(d);

        Encounter.Turn turn = encounter.nextTurn();

        assertSame(rekijan, turn.active);
        assertEquals(1, d.getDuration());
    }

    @Test
    public void saveRoundTripKeepsCastersAndSpellCards() {
        setUpParty();
        String bless = Spells.cast(encounter, mira, Arrays.asList(rekijan, tor), "Bless", 2, "");
        Spells.cast(encounter, rekijan, Collections.singletonList(ash), "Haste", 3, "");
        encounter.remove(mira);

        List<CharacterModel> loaded = EncounterJson.fromJson(EncounterJson.toJson(encounter.getCharacters()));
        Encounter reloaded = new Encounter(loaded, 1);

        CharacterModel card = loaded.get(0);
        assertTrue(card.isSpellCard());
        assertEquals("Mira", card.getSpellCardCaster());
        assertEquals(card.getStableId(), encounter.get(0).getStableId());
        DebuffModel loadedBless = loaded.get(1).getDebuffList().get(0);
        assertEquals(card.getStableId(), loadedBless.getCasterId());
        assertEquals("Mira", loadedBless.getCasterName());
        assertEquals(bless, loadedBless.getSpellId());
        assertEquals(loaded.get(1).getStableId(), loaded.get(3).getDebuffList().get(0).getCasterId());

        // Still counts on the right turn after loading: Rekijan's turn ticks Haste on Ash
        reloaded.nextTurn(); // Rekijan
        assertEquals(2, loaded.get(3).getDebuffList().get(0).getDuration());
        assertEquals(2, loadedBless.getDuration());
    }

    @Test
    public void oldSaveWithoutCastersLoadsAndCountsAsBefore() {
        String save = "[{\"id\":0,\"initiative\":18,\"isFirstInRound\":true,\"characterName\":\"Merisiel\","
                + "\"debuffList\":[{\"name\":\"Haste\",\"duration\":3,\"description\":\"\"}]},"
                + "{\"id\":0,\"initiative\":9,\"characterName\":\"Goblin\","
                + "\"debuffList\":[{\"name\":\"Shaken\",\"duration\":2,\"description\":\"\"}]}]";
        List<CharacterModel> loaded = EncounterJson.fromJson(save);
        Encounter e = new Encounter(loaded, 1);

        DebuffModel haste = loaded.get(0).getDebuffList().get(0);
        assertFalse(haste.hasCaster());
        assertNull(haste.getSpellId());
        assertEquals("", haste.getCasterName());
        assertFalse(loaded.get(0).isSpellCard());
        // Every character gets its own stable id, though 1.x saved "id":0 for all
        assertNotEquals(loaded.get(0).getStableId(), loaded.get(1).getStableId());

        e.nextTurn(); // Goblin
        assertEquals(1, loaded.get(1).getDebuffList().get(0).getDuration());
        assertEquals(3, haste.getDuration());
        e.nextTurn(); // Merisiel
        assertEquals(2, haste.getDuration());
    }

    @Test
    public void syncSendsTheCasterNameAndTheGmKeepsTheCaster() {
        setUpParty();
        Spells.cast(encounter, mira, Collections.singletonList(rekijan), "Bless", 3, "");
        rekijan.getDebuffList().add(new DebuffModel("Prone", 0, ""));

        // To the player: the name only
        Map<String, Object> map = PlayerSheet.of(rekijan).toMap();
        PlayerSheet player = PlayerSheet.read(map);
        assertEquals("Mira", player.conditions.get(0).getCasterName());
        assertFalse(player.conditions.get(0).hasCaster());
        assertEquals("", player.conditions.get(1).getCasterName());

        // The player changes the rounds; the GM approves: still Mira's spell
        player.conditions.get(0).setDuration(1);
        PlayerSheet.read(player.toMap()).applyTo(rekijan);
        DebuffModel bless = rekijan.getDebuffList().get(0);
        assertEquals(1, bless.getDuration());
        assertEquals(mira.getStableId(), bless.getCasterId());
        assertNotNull(bless.getSpellId());
        assertFalse(rekijan.getDebuffList().get(1).hasCaster());
    }
}
