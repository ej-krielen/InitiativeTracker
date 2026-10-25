package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link SyncStore}: the hosted session, the applied sends and the player's form and
 * sentAt are saved and read back, and a new session starts without applied sends.
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
    public void playerFormAndCodeAreSaved() {
        store.setPlayerCode("K7QX4M");
        store.savePlayerSheet(PlayerSheetTest.valeros());
        SyncStore reopened = new SyncStore(values);
        assertEquals("K7QX4M", reopened.getPlayerCode());
        assertEquals("Valeros", reopened.loadPlayerSheet().name);
        assertEquals(34, reopened.loadPlayerSheet().maxHp);
        reopened.setPlayerCode(null);
        assertNull(reopened.getPlayerCode());
    }

    @Test
    public void emptyFormWhenNothingSaved() {
        assertEquals("", store.loadPlayerSheet().name);
    }

    @Test
    public void sentAtAlwaysGoesUp() {
        assertEquals(5000, store.nextSentAt(5000));
        store.setPlayerSentAt(5000);
        // The clock was set back: still later than the last send
        assertEquals(5001, store.nextSentAt(100));
        assertEquals(9000, store.nextSentAt(9000));
    }
}
