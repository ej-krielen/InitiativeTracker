package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The rules for player characters that are synced, or could be. Plain Java, so every case is
 * unit tested; {@link SyncManager} and the screens follow them.
 * <ul>
 *     <li>A card shows whether its PC is synced ({@link #status}): the player is in the session,
 *     the player left (still linked, so they merge when they come back), or not synced at all.</li>
 *     <li>The GM removes or unlinks a synced PC: the player gets a notice in the GM's copy
 *     ({@link GmCopy.Ending}) that answers everything they sent so far ({@link #noticeSentAt}),
 *     and their character leaves the session.</li>
 *     <li>The player only acts on a notice that came after they last joined
 *     ({@link #endsPlayer}), so coming back doesn't kick them out again.</li>
 *     <li>A player asking to join can be linked to a PC the GM ran so far, instead of being added
 *     twice ({@link #linkCandidates}, {@link #defaultLink}).</li>
 * </ul>
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2026
 */
public final class SyncRules {

    /** What a card shows about Table sync. */
    public enum Status {
        /** Not synced, or not hosting this party: nothing to show. */
        NONE,
        /** The player is in the session. */
        SYNCED,
        /** Linked to a player who isn't in the session (left, or deleted their character). */
        PLAYER_LEFT
    }

    private SyncRules() {
    }

    /** @return True when this sync id belongs to a player (it has their uid). */
    public static boolean isLinked(String syncId) {
        return !SyncIds.ownerUid(syncId).isEmpty();
    }

    /**
     * @param hostingThisParty True when hosting a session for the party this character is in, and it is open.
     * @param playersLoaded    True once the players in the session are known; until then a link counts as synced.
     * @param present          The document ids of the players in the session.
     */
    public static Status status(String syncId, boolean hostingThisParty, boolean playersLoaded, Set<String> present) {
        if (!hostingThisParty || !isLinked(syncId)) return Status.NONE;
        if (!playersLoaded || present.contains(syncId)) return Status.SYNCED;
        return Status.PLAYER_LEFT;
    }

    /**
     * The send a removal or unlink notice answers: the player's latest one the GM knows of, so the
     * player's app sees the notice as an answer to everything sent in this join.
     *
     * @param lastSent The sentAt of the player's sheet in the session, or null when it isn't there.
     * @param answered The sentAt the GM last answered, or null when none.
     */
    public static long noticeSentAt(Long lastSent, Long answered) {
        long a = lastSent != null ? lastSent : 0;
        long b = answered != null ? answered : 0;
        return Math.max(0, Math.max(a, b));
    }

    /**
     * @param joinFloor The character's sentAt when it last joined: notices for earlier sends are old.
     * @return True when this copy tells the player the GM removed or unlinked them since they joined.
     */
    public static boolean endsPlayer(GmCopy copy, long joinFloor) {
        return copy != null && copy.ended != GmCopy.Ending.NONE && copy.answeredSentAt > joinFloor;
    }

    /**
     * The PCs a player asking to join can be linked to: PCs the GM runs (no player), and PCs whose
     * player isn't in the session. A PC linked to a player who is there is never offered. A PC
     * with the same name as the send comes first; otherwise the party's order is kept.
     *
     * @param present The document ids of the players in the session.
     */
    public static List<CharacterModel> linkCandidates(List<CharacterModel> characters, Set<String> present,
                                                      String requestDocId, String name) {
        List<CharacterModel> matches = new ArrayList<>();
        List<CharacterModel> others = new ArrayList<>();
        for (CharacterModel c : characters) {
            if (!c.isPC()) continue;
            String syncId = c.getSyncId();
            if (isLinked(syncId) && (present.contains(syncId) || syncId.equals(requestDocId))) continue;
            if (sameName(c.getCharacterName(), name)) {
                matches.add(c);
            } else {
                others.add(c);
            }
        }
        matches.addAll(others);
        return matches;
    }

    /** @return The index in the candidates to pick at first: a PC with the same name, or -1 to add a new PC. */
    public static int defaultLink(List<CharacterModel> candidates, String name) {
        for (int i = 0; i < candidates.size(); i++) {
            if (sameName(candidates.get(i).getCharacterName(), name)) return i;
        }
        return -1;
    }

    private static boolean sameName(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.trim().toLowerCase(Locale.ROOT);
        return !x.isEmpty() && x.equals(b.trim().toLowerCase(Locale.ROOT));
    }
}
