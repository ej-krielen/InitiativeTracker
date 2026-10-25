package com.rekijan.initiativetracker.feature.party;

import java.util.Collection;
import java.util.Locale;

/**
 * The naming rules for parties: a name is trimmed, 1 to {@link #MAX_LENGTH} characters long, and
 * unique (ignoring case). Also makes names for a copy ("Thursday group 2") and for a new party
 * ("Party 3"). Plain Java, so the rules are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class PartyNames {

    public static final int MAX_LENGTH = 30;

    /** The outcome of {@link #check}. */
    public enum Check {
        OK, EMPTY, TOO_LONG, DUPLICATE
    }

    private PartyNames() {
    }

    /** @return The name as it is saved: trimmed, never null. */
    public static String clean(String name) {
        return name == null ? "" : name.trim();
    }

    /**
     * @param excludeId The party being renamed, so it may keep its own name; null for a new party.
     */
    public static Check check(String name, Collection<Party> parties, String excludeId) {
        String cleaned = clean(name);
        if (cleaned.isEmpty()) return Check.EMPTY;
        if (cleaned.length() > MAX_LENGTH) return Check.TOO_LONG;
        if (isTaken(cleaned, parties, excludeId)) return Check.DUPLICATE;
        return Check.OK;
    }

    /** @return "Name 2", or "Name 3" when that is taken too, cut so it fits {@link #MAX_LENGTH}. */
    public static String copyName(String base, Collection<Party> parties) {
        return numbered(clean(base), 2, parties);
    }

    /**
     * @param prefix The word for a party in the user's language, like "Party".
     * @return "Party 1", or the first free number after it.
     */
    public static String newName(String prefix, Collection<Party> parties) {
        return numbered(clean(prefix), 1, parties);
    }

    private static String numbered(String base, int first, Collection<Party> parties) {
        for (int n = first; ; n++) {
            String suffix = " " + n;
            String head = base.length() + suffix.length() > MAX_LENGTH
                    ? base.substring(0, MAX_LENGTH - suffix.length()).trim()
                    : base;
            String candidate = head + suffix;
            if (!isTaken(candidate, parties, null)) return candidate;
        }
    }

    private static boolean isTaken(String name, Collection<Party> parties, String excludeId) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (Party p : parties) {
            if (p.getId().equals(excludeId)) continue;
            if (clean(p.getName()).toLowerCase(Locale.ROOT).equals(lower)) return true;
        }
        return false;
    }
}
