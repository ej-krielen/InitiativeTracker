package com.rekijan.initiativetracker.feature.sync;

/**
 * The Firestore document id of a player character in a session: the player's Firebase uid, an
 * underscore, and the character's id on their phone. So one phone can send several characters
 * to the same session, and the rules can still check that a player only writes their own: the
 * part before the underscore must be their uid. The GM's copy uses the same id. Plain Java, so it
 * is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SyncIds {

    private SyncIds() {
    }

    /** @return The document id for this player's character. */
    public static String playerDocId(String uid, String characterId) {
        return uid + "_" + characterId;
    }

    /** @return The uid part of a document id, or "" when it has none. */
    public static String ownerUid(String docId) {
        if (docId == null) return "";
        int underscore = docId.indexOf('_');
        return underscore > 0 ? docId.substring(0, underscore) : "";
    }
}
