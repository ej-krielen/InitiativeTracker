package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests {@link SyncIds}: a player character's document id joins the uid and the character id,
 * and the uid can be read back, as the Firestore rules do.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncIdsTest {

    @Test
    public void docIdStartsWithTheUid() {
        String docId = SyncIds.playerDocId("abc123XYZ", "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed");
        assertEquals("abc123XYZ_1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed", docId);
        assertEquals("abc123XYZ", SyncIds.ownerUid(docId));
    }

    @Test
    public void idWithoutUidHasNoOwner() {
        assertEquals("", SyncIds.ownerUid("abc123XYZ"));
        assertEquals("", SyncIds.ownerUid("_character"));
        assertEquals("", SyncIds.ownerUid(null));
    }
}
