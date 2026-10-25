package com.rekijan.initiativetracker.feature.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests {@link PartySlots}: 5 free parties, 5 more per pack, 4 packs at most.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartySlotsTest {

    @Test
    public void fiveFreeAndFiveMorePerPack() {
        assertEquals(5, PartySlots.limit(0));
        assertEquals(10, PartySlots.limit(1));
        assertEquals(25, PartySlots.limit(4));
    }

    @Test
    public void levelIsClamped() {
        assertEquals(25, PartySlots.limit(9));
        assertEquals(5, PartySlots.limit(-1));
    }

    @Test
    public void canCreateUntilTheLimit() {
        assertTrue(PartySlots.canCreate(4, 0));
        assertFalse(PartySlots.canCreate(5, 0));
        assertTrue(PartySlots.canCreate(5, 1));
        // Over the limit after a refund: no new parties, existing ones are kept elsewhere
        assertFalse(PartySlots.canCreate(8, 0));
    }

    @Test
    public void nextPackStopsAfterTheLast() {
        assertEquals(1, PartySlots.nextPackLevel(0));
        assertEquals(4, PartySlots.nextPackLevel(3));
        assertEquals(0, PartySlots.nextPackLevel(4));
    }
}
