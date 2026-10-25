package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests {@link SyncForm}: numbers on the player's form are read when they are whole numbers in
 * range, and refused otherwise, so the player can be told the range.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncFormTest {

    @Test
    public void readsNumbersInRange() {
        assertEquals(Integer.valueOf(18), SyncForm.readNumber("18", -99, 999));
        assertEquals(Integer.valueOf(-3), SyncForm.readNumber(" -3 ", -99, 999));
        assertEquals(Integer.valueOf(2), SyncForm.readNumber("+2", -99, 999));
        assertEquals(Integer.valueOf(999), SyncForm.readNumber("999", -99, 999));
    }

    @Test
    public void emptyIsZeroWhenZeroIsAllowed() {
        assertEquals(Integer.valueOf(0), SyncForm.readNumber("", -99, 999));
        assertEquals(Integer.valueOf(0), SyncForm.readNumber(null, 0, 99999));
        assertNull(SyncForm.readNumber("", 1, 10));
    }

    @Test
    public void refusesOutOfRangeAndGarbage() {
        assertNull(SyncForm.readNumber("1000", -99, 999));
        assertNull(SyncForm.readNumber("-100", -99, 999));
        assertNull(SyncForm.readNumber("-", -99, 999));
        assertNull(SyncForm.readNumber("12a", -99, 999));
        assertNull(SyncForm.readNumber("1.5", -99, 999));
        assertNull(SyncForm.readNumber("99999999999999", -99999, 99999));
    }
}
