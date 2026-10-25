package com.rekijan.initiativetracker.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests {@link NumberInput}: whatever the user types gives a number inside the range, without
 * crashing.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class NumberInputTest {

    @Test
    public void parsesNormalNumbers() {
        assertEquals(12, NumberInput.parseInt("12", 0, -99, 999));
        assertEquals(-3, NumberInput.parseInt("-3", 0, -99, 999));
        assertEquals(7, NumberInput.parseInt(" 7 ", 0, -99, 999));
    }

    @Test
    public void emptyOrInvalidTextGivesFallback() {
        assertEquals(0, NumberInput.parseInt(null, 0, -99, 999));
        assertEquals(0, NumberInput.parseInt("", 0, -99, 999));
        assertEquals(0, NumberInput.parseInt("-", 0, -99, 999));
        assertEquals(5, NumberInput.parseInt("abc", 5, -99, 999));
    }

    @Test
    public void hugeNumbersAreClampedNotCrashing() {
        assertEquals(999, NumberInput.parseInt("99999999999999999999999", 0, -99, 999));
        assertEquals(-99, NumberInput.parseInt("-99999999999999999999999", 0, -99, 999));
        assertEquals(999, NumberInput.parseInt("2147483648", 0, -99, 999));
    }

    @Test
    public void fallbackIsAlsoClamped() {
        assertEquals(1, NumberInput.parseInt("", 0, 1, 10));
    }
}
