package com.rekijan.initiativetracker.core.util;

/**
 * Turns what the user typed into a number without crashing: empty text, a lone minus sign,
 * garbage and numbers too big for an int all give a safe value, and the result is kept inside
 * the range the field allows. Plain Java so it can be unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class NumberInput {

    private NumberInput() {
    }

    /**
     * @param text     What the user typed, may be null.
     * @param fallback Returned when the text holds no number at all.
     * @param min      Lowest allowed value.
     * @param max      Highest allowed value.
     * @return The number in the text, clamped to [min, max], or the fallback (also clamped).
     */
    public static int parseInt(String text, int fallback, int min, int max) {
        long value = fallback;
        if (text != null) {
            String trimmed = text.trim();
            if (!trimmed.isEmpty()) {
                try {
                    value = Long.parseLong(trimmed);
                } catch (NumberFormatException e) {
                    // Too many digits for a long still means "a very big number"
                    if (trimmed.matches("-?\\d+")) {
                        value = trimmed.startsWith("-") ? Long.MIN_VALUE : Long.MAX_VALUE;
                    }
                }
            }
        }
        return clamp(value, min, max);
    }

    /** @return The value, kept inside [min, max]. */
    public static int clamp(long value, int min, int max) {
        return (int) Math.max(min, Math.min(max, value));
    }
}
