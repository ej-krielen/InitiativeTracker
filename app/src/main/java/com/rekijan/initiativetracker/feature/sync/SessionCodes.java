package com.rekijan.initiativetracker.feature.sync;

import java.util.Locale;
import java.util.Random;

/**
 * Makes and checks Table sync session codes: six characters from an alphabet without 0/O and
 * 1/I, so a code read out at the table can't be mistyped that way. The Firestore rules check the
 * same pattern. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SessionCodes {

    public static final int LENGTH = 6;
    /** Keep in sync with the code pattern in firestore.rules. */
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private SessionCodes() {
    }

    /** @return A new random code. */
    public static String generate(Random random) {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Cleans up what the player typed: spaces and dashes go, letters become upper case.
     *
     * @return The cleaned code, or "" for null.
     */
    public static String normalize(String typed) {
        if (typed == null) return "";
        return typed.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** @return True when the (normalized) code has the right length and only allowed characters. */
    public static boolean isValid(String code) {
        if (code == null || code.length() != LENGTH) return false;
        for (int i = 0; i < code.length(); i++) {
            if (ALPHABET.indexOf(code.charAt(i)) < 0) return false;
        }
        return true;
    }
}
