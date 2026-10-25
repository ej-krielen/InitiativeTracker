package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link GmCopy}: the GM's version of a character survives the trip to Firestore with the
 * answered send, a rejection says so, and a broken document is cleaned up.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class GmCopyTest {

    private static CharacterModel gmValeros() {
        CharacterModel c = new CharacterModel();
        c.setCharacterName("Valeros");
        c.setHp(12);
        c.setMaxHp(34);
        c.setFastHealing(2);
        c.setAc("18");
        c.getDebuffList().add(new DebuffModel("Shaken", 2, "-2 attacks"));
        return c;
    }

    @Test
    public void roundTripKeepsTheSheetAndTheAnswer() {
        GmCopy back = GmCopy.fromMap(GmCopy.of(gmValeros(), 5000).toMap());
        assertEquals("Valeros", back.sheet.name);
        assertEquals(12, back.sheet.hp);
        assertEquals(2, back.sheet.fastHealing);
        assertEquals("18", back.sheet.ac);
        assertEquals("Shaken", back.sheet.conditions.get(0).getName());
        assertEquals(5000, back.answeredSentAt);
        assertFalse(back.rejected);
    }

    @Test
    public void copyHasNoSentAtButAnAnswerAndRejected() {
        Map<String, Object> map = GmCopy.of(gmValeros(), 0).toMap();
        assertFalse(map.containsKey(PlayerSheet.KEY_SENT_AT));
        assertTrue(map.get(GmCopy.KEY_ANSWERED_SENT_AT) instanceof Long);
        assertEquals(false, map.get(GmCopy.KEY_REJECTED));
    }

    @Test
    public void rejectionAnswersTheSend() {
        PlayerSheet sent = PlayerSheet.of(gmValeros());
        sent.sentAt = 7000;
        GmCopy back = GmCopy.fromMap(GmCopy.rejection(sent).toMap());
        assertTrue(back.rejected);
        assertEquals(7000, back.answeredSentAt);
    }

    @Test
    public void emptyNameIsAllowedInTheGmsCopy() {
        CharacterModel c = gmValeros();
        c.setCharacterName("");
        assertEquals("", GmCopy.fromMap(GmCopy.of(c, 0).toMap()).sheet.name);
    }

    @Test
    public void brokenDocumentIsCleaned() {
        Map<String, Object> map = new HashMap<>();
        map.put(PlayerSheet.KEY_HP, 1e12);
        map.put(GmCopy.KEY_ANSWERED_SENT_AT, -5L);
        map.put(GmCopy.KEY_REJECTED, "yes");
        GmCopy copy = GmCopy.fromMap(map);
        assertEquals(CharacterModel.MAX_HP, copy.sheet.hp);
        assertEquals(0, copy.answeredSentAt);
        assertFalse(copy.rejected);
    }

    @Test
    public void noMapGivesNull() {
        assertNull(GmCopy.fromMap(null));
    }
}
