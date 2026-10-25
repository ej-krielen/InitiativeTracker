package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link SyncStore}: the hosted session and the applied sends are saved and read back, a
 * new session starts without applied sends, and each player character keeps its own session,
 * sentAt and syncedAt.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncStoreTest {

    private final MemoryKeyValueStore values = new MemoryKeyValueStore();
    private final SyncStore store = new SyncStore(values);

    @Test
    public void hostingIsSavedAndCleared() {
        store.startHosting("K7QX4M", "party-1");
        Map<String, Long> applied = new HashMap<>();
        applied.put("uid1", 1000L);
        store.saveApplied(applied);

        SyncStore reopened = new SyncStore(values);
        assertEquals("K7QX4M", reopened.getHostCode());
        assertEquals("party-1", reopened.getHostPartyId());
        assertEquals(1000L, (long) reopened.loadApplied().get("uid1"));

        reopened.stopHosting();
        assertNull(reopened.getHostCode());
        assertNull(reopened.getHostPartyId());
        assertTrue(reopened.loadApplied().isEmpty());
    }

    @Test
    public void newSessionForgetsAppliedSends() {
        store.startHosting("K7QX4M", "party-1");
        Map<String, Long> applied = new HashMap<>();
        applied.put("uid1", 1000L);
        store.saveApplied(applied);
        store.startHosting("ABCDEF", "party-2");
        assertTrue(store.loadApplied().isEmpty());
    }

    @Test
    public void unreadableAppliedIsEmpty() {
        values.edit().putString(SyncStore.KEY_HOST_APPLIED, "{not json").apply();
        assertTrue(store.loadApplied().isEmpty());
    }

    @Test
    public void eachPlayerCharacterHasItsOwnSession() {
        store.setPlayerCode("a", "K7QX4M");
        store.setPlayerCode("b", "ABCDEF");
        SyncStore reopened = new SyncStore(values);
        assertEquals("K7QX4M", reopened.getPlayerCode("a"));
        assertEquals("ABCDEF", reopened.getPlayerCode("b"));
        assertNull(reopened.getPlayerCode("c"));
    }

    @Test
    public void leavingForgetsSyncedButKeepsSentAt() {
        store.setPlayerCode("a", "K7QX4M");
        store.setPlayerSentAt("a", 5000);
        store.setPlayerSyncedAt("a", 6000);
        store.setPlayerCode("a", null);
        assertNull(store.getPlayerCode("a"));
        assertEquals(0, store.getPlayerSyncedAt("a"));
        // Kept, so a later send in a new session is still newer than the last one
        assertEquals(5000, store.getPlayerSentAt("a"));
    }

    @Test
    public void forgetPlayerRemovesEverything() {
        store.setPlayerCode("a", "K7QX4M");
        store.setPlayerSentAt("a", 5000);
        store.setPlayerSyncedAt("a", 6000);
        store.forgetPlayer("a");
        assertNull(store.getPlayerCode("a"));
        assertEquals(0, store.getPlayerSentAt("a"));
        assertEquals(0, store.getPlayerSyncedAt("a"));
    }

    @Test
    public void sentAtAlwaysGoesUp() {
        assertEquals(5000, store.nextSentAt("a", 5000));
        store.setPlayerSentAt("a", 5000);
        // The clock was set back: still later than the last send
        assertEquals(5001, store.nextSentAt("a", 100));
        assertEquals(9000, store.nextSentAt("a", 9000));
        // Another character has its own count
        assertEquals(100, store.nextSentAt("b", 100));
    }

    @Test
    public void baseSentAndAnsweredAreKeptPerCharacter() {
        PlayerSheet base = new PlayerSheet();
        base.name = "Valeros";
        base.hp = 20;
        store.savePlayerBase("a", base);
        PlayerSheet sent = base.cleaned();
        sent.hp = 30;
        sent.sentAt = 5000;
        store.savePlayerSent("a", sent);
        store.setPlayerAnswered("a", 4000);

        SyncStore reopened = new SyncStore(values);
        assertEquals(20, reopened.loadPlayerBase("a").hp);
        assertEquals(30, reopened.loadPlayerSent("a").hp);
        assertEquals(4000, reopened.getPlayerAnswered("a"));
        assertNull(reopened.loadPlayerBase("b"));

        reopened.clearPlayerSent("a");
        assertNull(reopened.loadPlayerSent("a"));
    }

    @Test
    public void leavingForgetsTheGmsVersionAndWhatWasSent() {
        store.setPlayerCode("a", "K7QX4M");
        store.savePlayerBase("a", new PlayerSheet());
        store.savePlayerSent("a", new PlayerSheet());
        store.setPlayerCode("a", null);
        assertNull(store.loadPlayerBase("a"));
        assertNull(store.loadPlayerSent("a"));
    }
}
