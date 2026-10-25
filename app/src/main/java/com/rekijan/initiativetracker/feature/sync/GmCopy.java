package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.Map;

/**
 * The GM's version of a player's character. The GM is the final arbiter, so the GM's app uploads
 * it whenever that character changes in the hosted party (damage, a condition, an approved or
 * refused request), and the player's sheet follows it live ({@link SheetMerge}).
 * <p>
 * Besides the whole sheet it says which of the player's sends the GM answered last
 * ({@link #answeredSentAt}), so the player's app knows its request was decided, and whether the
 * GM rejected the character joining ({@link #rejected}). Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class GmCopy {

    static final String KEY_ANSWERED_SENT_AT = "answeredSentAt";
    static final String KEY_REJECTED = "rejected";

    public PlayerSheet sheet = new PlayerSheet();
    /** The sentAt of the player's send the GM last approved or refused; 0 when none yet. */
    public long answeredSentAt;
    /** True when the GM didn't let this character join. */
    public boolean rejected;

    /** @return The GM's copy of this character. */
    public static GmCopy of(CharacterModel character, long answeredSentAt) {
        GmCopy copy = new GmCopy();
        copy.sheet = PlayerSheet.of(character);
        copy.answeredSentAt = Math.max(0, answeredSentAt);
        return copy;
    }

    /** @return A copy that tells the player the GM didn't let them join. */
    public static GmCopy rejection(PlayerSheet sent) {
        GmCopy copy = new GmCopy();
        copy.sheet = sent.cleaned();
        copy.answeredSentAt = sent.sentAt;
        copy.rejected = true;
        return copy;
    }

    /** @return The copy as a Firestore document (numbers as Long), without the expiry. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = sheet.toMap();
        map.remove(PlayerSheet.KEY_SENT_AT);
        map.put(KEY_ANSWERED_SENT_AT, Math.max(0, answeredSentAt));
        map.put(KEY_REJECTED, rejected);
        return map;
    }

    /**
     * Reads a received copy. Like a player's sheet it is cleaned up, so a broken document can't
     * break the player's sheet; a name may be empty here.
     *
     * @return The copy, or null when there is no map.
     */
    public static GmCopy fromMap(Map<String, Object> map) {
        if (map == null) return null;
        GmCopy copy = new GmCopy();
        copy.sheet = PlayerSheet.read(map);
        copy.sheet.sentAt = 0;
        Object answered = map.get(KEY_ANSWERED_SENT_AT);
        copy.answeredSentAt = answered instanceof Number ? Math.max(0, ((Number) answered).longValue()) : 0;
        copy.rejected = Boolean.TRUE.equals(map.get(KEY_REJECTED));
        return copy;
    }
}
