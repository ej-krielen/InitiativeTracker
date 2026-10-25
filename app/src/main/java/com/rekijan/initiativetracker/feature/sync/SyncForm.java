package com.rekijan.initiativetracker.feature.sync;

/**
 * Checks the numbers on the player's Table sync form. Unlike the character details (which clamp
 * silently), the form says when a number is out of range, so a player never sends a value they
 * didn't mean. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SyncForm {

    private SyncForm() {
    }

    /**
     * @param text What the player typed; empty means 0.
     * @return The number, or null when it is not a whole number inside [min, max].
     */
    public static Integer readNumber(String text, int min, int max) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) return min <= 0 && max >= 0 ? 0 : null;
        if (trimmed.startsWith("+")) trimmed = trimmed.substring(1);
        if (!trimmed.matches("-?\\d{1,9}")) return null;
        int value = Integer.parseInt(trimmed);
        return value >= min && value <= max ? value : null;
    }
}
