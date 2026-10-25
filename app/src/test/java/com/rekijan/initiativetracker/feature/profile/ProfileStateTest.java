package com.rekijan.initiativetracker.feature.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.core.data.MemoryKeyValueStore;

import org.junit.Test;

/**
 * Tests {@link ProfileState}: a fresh install (and the 1.x update) opens the party, and the last
 * opened profile is remembered.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class ProfileStateTest {

    private final MemoryKeyValueStore values = new MemoryKeyValueStore();

    @Test
    public void freshInstallOpensTheParty() {
        ProfileState state = new ProfileState(values);
        assertFalse(state.isPlayer());
        assertNull(state.getPlayerId());
    }

    @Test
    public void lastOpenedProfileIsRemembered() {
        new ProfileState(values).openPlayer("abc");
        ProfileState reopened = new ProfileState(values);
        assertTrue(reopened.isPlayer());
        assertEquals("abc", reopened.getPlayerId());

        reopened.openParty();
        assertFalse(new ProfileState(values).isPlayer());
        assertNull(new ProfileState(values).getPlayerId());
    }
}
