package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * Tests {@link SessionCodes}: generated codes are valid, typed codes are cleaned up, and codes
 * with look-alike characters or the wrong length are refused.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SessionCodesTest {

    @Test
    public void generatedCodesAreValid() {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            String code = SessionCodes.generate(random);
            assertEquals(SessionCodes.LENGTH, code.length());
            assertTrue(code, SessionCodes.isValid(code));
        }
    }

    @Test
    public void normalizeRemovesSpacesAndDashesAndUpperCases() {
        assertEquals("K7QX4M", SessionCodes.normalize(" k7q-x4m "));
        assertEquals("K7QX4M", SessionCodes.normalize("K7Q X4M"));
        assertEquals("", SessionCodes.normalize(null));
    }

    @Test
    public void lookAlikesAndWrongLengthsAreInvalid() {
        assertFalse(SessionCodes.isValid("K7QX4O")); // letter O
        assertFalse(SessionCodes.isValid("K7QX40")); // digit 0
        assertFalse(SessionCodes.isValid("K7QX4I"));
        assertFalse(SessionCodes.isValid("K7QX41"));
        assertFalse(SessionCodes.isValid("K7QX4"));
        assertFalse(SessionCodes.isValid("K7QX4MM"));
        assertFalse(SessionCodes.isValid("k7qx4m"));
        assertFalse(SessionCodes.isValid(null));
    }
}
