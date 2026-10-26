package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link SyncRules}, one test per case of a synced PC: the card status, the GM removing or
 * unlinking, the player coming back, the player leaving, and linking a PC the GM ran so far.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2026
 */
public class SyncRulesTest {

    private static final String VALEROS = "uidA_char1";
    private static final String SEONI = "uidB_char2";
    private static final String KYRA = "uidC_char3";

    private static CharacterModel pc(String name, String syncId) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        c.setPC(true);
        c.setSyncId(syncId);
        return c;
    }

    private static Set<String> present(String... docIds) {
        return new HashSet<>(Arrays.asList(docIds));
    }

    private static GmCopy notice(GmCopy.Ending ending, long answered) {
        return GmCopy.notice(pc("Valeros", VALEROS), answered, ending);
    }

    // ------------------------------------------------------------ Card status

    @Test
    public void playerInTheSessionIsSynced() {
        assertEquals(SyncRules.Status.SYNCED, SyncRules.status(VALEROS, true, true, present(VALEROS)));
    }

    @Test
    public void playerWhoLeftShowsPlayerLeft() {
        assertEquals(SyncRules.Status.PLAYER_LEFT, SyncRules.status(VALEROS, true, true, present(SEONI)));
    }

    @Test
    public void untilThePlayersAreKnownALinkCountsAsSynced() {
        assertEquals(SyncRules.Status.SYNCED, SyncRules.status(VALEROS, true, false, present()));
    }

    @Test
    public void gmRunPcShowsNothing() {
        assertEquals(SyncRules.Status.NONE, SyncRules.status(null, true, true, present(VALEROS)));
        assertEquals(SyncRules.Status.NONE, SyncRules.status("", true, true, present()));
        assertEquals(SyncRules.Status.NONE, SyncRules.status("no-uid", true, true, present()));
    }

    @Test
    public void notHostingThisPartyShowsNothing() {
        assertEquals(SyncRules.Status.NONE, SyncRules.status(VALEROS, false, true, present(VALEROS)));
    }

    // ------------------------------------------------------------ GM removes or unlinks

    @Test
    public void noticeAnswersTheLatestSendTheGmKnows() {
        assertEquals(3000, SyncRules.noticeSentAt(3000L, 2000L));
        assertEquals(2000, SyncRules.noticeSentAt(null, 2000L));
        assertEquals(3000, SyncRules.noticeSentAt(3000L, null));
        assertEquals(0, SyncRules.noticeSentAt(null, null));
    }

    @Test
    public void removalEndsThePlayer() {
        // Joined at floor 0, sent at 1000, then removed
        assertTrue(SyncRules.endsPlayer(notice(GmCopy.Ending.REMOVED, 1000), 0));
    }

    @Test
    public void unlinkEndsThePlayer() {
        assertTrue(SyncRules.endsPlayer(notice(GmCopy.Ending.UNLINKED, 1000), 0));
    }

    @Test
    public void ordinaryCopyNeverEndsThePlayer() {
        assertFalse(SyncRules.endsPlayer(GmCopy.of(pc("Valeros", VALEROS), 5000), 0));
        assertFalse(SyncRules.endsPlayer(null, 0));
    }

    @Test
    public void removalWhileASendWaitsStillEndsThePlayer() {
        // The player sent 1000 (answered) and 2000 (waiting); the notice answers 2000
        long answered = SyncRules.noticeSentAt(2000L, 1000L);
        assertTrue(SyncRules.endsPlayer(notice(GmCopy.Ending.REMOVED, answered), 0));
    }

    // ------------------------------------------------------------ Player comes back

    @Test
    public void oldRemovalDoesNotEndThePlayerAfterJoiningAgain() {
        SyncStore store = new SyncStore(new MemoryKeyValueStore());
        store.setPlayerCode("char1", "K7QX4M");
        store.setPlayerSentAt("char1", 1000);
        GmCopy removal = notice(GmCopy.Ending.REMOVED, 1000);
        assertTrue(SyncRules.endsPlayer(removal, store.getPlayerJoinFloor("char1")));

        // Kicked out, joins again: the same notice is still online but old now
        store.setPlayerCode("char1", null);
        store.setPlayerCode("char1", "K7QX4M");
        assertFalse(SyncRules.endsPlayer(removal, store.getPlayerJoinFloor("char1")));

        // Sends again and is removed again: that notice counts
        store.setPlayerSentAt("char1", 2000);
        assertTrue(SyncRules.endsPlayer(notice(GmCopy.Ending.REMOVED, 2000), store.getPlayerJoinFloor("char1")));
    }

    @Test
    public void playerWhoLeftThemselvesIsNotEndedByAnOldRemoval() {
        // Removed while away (never saw it), left, joins again: the removal is from before the join
        SyncStore store = new SyncStore(new MemoryKeyValueStore());
        store.setPlayerCode("char1", "K7QX4M");
        store.setPlayerSentAt("char1", 1000);
        store.setPlayerCode("char1", null);
        store.setPlayerCode("char1", "K7QX4M");
        assertFalse(SyncRules.endsPlayer(notice(GmCopy.Ending.REMOVED, 1000), store.getPlayerJoinFloor("char1")));
    }

    @Test
    public void gmSeesThatAReturningPlayerWasRemovedFromThisParty() {
        SyncStore store = new SyncStore(new MemoryKeyValueStore());
        store.markRemoved(VALEROS, "party-1");
        assertTrue(store.wasRemoved(VALEROS, "party-1"));
        assertFalse(store.wasRemoved(VALEROS, "party-2"));
        assertFalse(store.wasRemoved(SEONI, "party-1"));

        // Added again: the next request no longer says so
        store.clearRemoved(VALEROS);
        assertFalse(store.wasRemoved(VALEROS, "party-1"));
    }

    @Test
    public void removalIsRememberedAcrossSessions() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        SyncStore store = new SyncStore(values);
        store.startHosting("K7QX4M", "party-1");
        store.markRemoved(VALEROS, "party-1");
        store.stopHosting();
        store.startHosting("ABCDEF", "party-1");
        assertTrue(new SyncStore(values).wasRemoved(VALEROS, "party-1"));
    }

    @Test
    public void unreadableRemovalsAreEmpty() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit().putString(SyncStore.KEY_HOST_REMOVED, "{not json").apply();
        assertFalse(new SyncStore(values).wasRemoved(VALEROS, "party-1"));
    }

    // ------------------------------------------------------------ Linking a PC the GM ran so far

    @Test
    public void gmRunPcWithTheSameNameIsOfferedFirst() {
        CharacterModel seoni = pc("Seoni", null);
        CharacterModel kyra = pc("Kyra", null);
        List<CharacterModel> candidates = SyncRules.linkCandidates(Arrays.asList(seoni, kyra),
                present(), KYRA, " kyra ");
        assertEquals(Arrays.asList(kyra, seoni), candidates);
        assertEquals(0, SyncRules.defaultLink(candidates, " kyra "));
    }

    @Test
    public void pcWhosePlayerLeftIsOffered() {
        CharacterModel seoni = pc("Seoni", SEONI);
        assertEquals(Collections.singletonList(seoni),
                SyncRules.linkCandidates(Collections.singletonList(seoni), present(VALEROS), KYRA, "Kyra"));
    }

    @Test
    public void pcWhosePlayerIsThereIsNotOffered() {
        CharacterModel valeros = pc("Valeros", VALEROS);
        assertTrue(SyncRules.linkCandidates(Collections.singletonList(valeros), present(VALEROS), KYRA, "Valeros")
                .isEmpty());
    }

    @Test
    public void npcsAreNotOffered() {
        CharacterModel goblin = pc("Goblin", null);
        goblin.setPC(false);
        assertTrue(SyncRules.linkCandidates(Collections.singletonList(goblin), present(), KYRA, "Goblin").isEmpty());
    }

    @Test
    public void withoutAMatchingNameTheDefaultIsANewPc() {
        List<CharacterModel> candidates = SyncRules.linkCandidates(
                Collections.singletonList(pc("Seoni", null)), present(), KYRA, "Kyra");
        assertEquals(-1, SyncRules.defaultLink(candidates, "Kyra"));
        assertEquals(-1, SyncRules.defaultLink(Collections.emptyList(), "Kyra"));
    }

    @Test
    public void unnamedPcIsNeverANameMatch() {
        CharacterModel unnamed = pc("", null);
        List<CharacterModel> candidates = SyncRules.linkCandidates(
                Collections.singletonList(unnamed), present(), KYRA, "");
        assertSame(unnamed, candidates.get(0));
        assertEquals(-1, SyncRules.defaultLink(candidates, ""));
    }

    @Test
    public void linkIsWhatTheSyncIdSays() {
        assertTrue(SyncRules.isLinked(VALEROS));
        assertFalse(SyncRules.isLinked(null));
        assertFalse(SyncRules.isLinked("_char1"));
    }
}
