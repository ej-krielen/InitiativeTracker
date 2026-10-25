package com.rekijan.initiativetracker.feature.party;

/**
 * How many parties a user may have: {@link #FREE} for everyone, plus {@link #PER_PACK} for each
 * party pack level. The packs are four one-time products (party_pack_1 to party_pack_4, see
 * BillingManager), like the Spacious Pouches in Secret Roller; only the highest one owned counts.
 * <p>
 * The limit only blocks making new parties. Parties over the limit (after a refund) stay usable,
 * so nobody loses data.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class PartySlots {

    public static final int FREE = 5;
    public static final int PER_PACK = 5;
    public static final int MAX_PACK_LEVEL = 4;

    private PartySlots() {
    }

    /** @return The pack level kept between 0 and {@link #MAX_PACK_LEVEL}. */
    public static int clampLevel(int packLevel) {
        return Math.max(0, Math.min(MAX_PACK_LEVEL, packLevel));
    }

    public static int limit(int packLevel) {
        return FREE + PER_PACK * clampLevel(packLevel);
    }

    public static boolean canCreate(int used, int packLevel) {
        return used < limit(packLevel);
    }

    /** @return The pack to offer next (1 to 4), or 0 when the biggest one is owned. */
    public static int nextPackLevel(int packLevel) {
        int level = clampLevel(packLevel);
        return level < MAX_PACK_LEVEL ? level + 1 : 0;
    }
}
