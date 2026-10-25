package com.rekijan.initiativetracker.feature.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests {@link PartyNames}: name checks and the names for copies and new parties.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartyNamesTest {

    private final List<Party> parties = Arrays.asList(
            new Party("a", "Thursday group"),
            new Party("b", "Party 1"));

    @Test
    public void acceptsAndTrimsAName() {
        assertEquals(PartyNames.Check.OK, PartyNames.check("  Curse of Strahd ", parties, null));
        assertEquals("Curse of Strahd", PartyNames.clean("  Curse of Strahd "));
    }

    @Test
    public void rejectsEmptyAndBlankAndNull() {
        assertEquals(PartyNames.Check.EMPTY, PartyNames.check("", parties, null));
        assertEquals(PartyNames.Check.EMPTY, PartyNames.check("   ", parties, null));
        assertEquals(PartyNames.Check.EMPTY, PartyNames.check(null, parties, null));
    }

    @Test
    public void rejectsTooLong() {
        String max = "x".repeat(PartyNames.MAX_LENGTH);
        assertEquals(PartyNames.Check.OK, PartyNames.check(max, parties, null));
        assertEquals(PartyNames.Check.TOO_LONG, PartyNames.check(max + "x", parties, null));
    }

    @Test
    public void rejectsDuplicateIgnoringCaseAndSpaces() {
        assertEquals(PartyNames.Check.DUPLICATE, PartyNames.check(" thursday GROUP", parties, null));
    }

    @Test
    public void renameMayKeepItsOwnName() {
        assertEquals(PartyNames.Check.OK, PartyNames.check("Thursday Group", parties, "a"));
        assertEquals(PartyNames.Check.DUPLICATE, PartyNames.check("Party 1", parties, "a"));
    }

    @Test
    public void copyNameCountsUp() {
        assertEquals("Thursday group 2", PartyNames.copyName("Thursday group", parties));
        List<Party> withCopy = Arrays.asList(parties.get(0), new Party("c", "Thursday group 2"));
        assertEquals("Thursday group 3", PartyNames.copyName("Thursday group", withCopy));
    }

    @Test
    public void copyNameOfALongNameStillFits() {
        String long30 = "y".repeat(PartyNames.MAX_LENGTH);
        String copy = PartyNames.copyName(long30, Collections.singletonList(new Party("d", long30)));
        assertTrue(copy.length() <= PartyNames.MAX_LENGTH);
        assertTrue(copy.endsWith(" 2"));
    }

    @Test
    public void newNameTakesTheFirstFreeNumber() {
        assertEquals("Party 2", PartyNames.newName("Party", parties));
        assertEquals("Party 1", PartyNames.newName("Party", Collections.emptyList()));
    }
}
