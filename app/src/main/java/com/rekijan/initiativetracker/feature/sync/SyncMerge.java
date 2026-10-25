package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Puts the players' sends into the GM's party. Firestore delivers every player's latest sheet
 * each time anything in the session changes, so only a sheet that was sent after the one last
 * applied for that player counts:
 * <ul>
 *     <li>A player whose character (same {@link CharacterModel#getSyncId()}) is in the party:
 *     the character is updated in place, keeping its (de)buffs, healing and turn order.</li>
 *     <li>A player without a character in the party: a new PC is added. This also brings back a
 *     character the GM deleted, but only when that player sends again.</li>
 *     <li>A sheet that was already applied: nothing changes, so the GM's own edits (damage,
 *     a changed initiative) are not undone.</li>
 * </ul>
 * Plain Java, so these rules are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SyncMerge {

    /** What a merge changed. */
    public static final class Result {
        /** New characters for the caller to add to the party, in no particular order. */
        public final List<CharacterModel> added = new ArrayList<>();
        /** Characters that were already in the party and were updated in place. */
        public final List<CharacterModel> updated = new ArrayList<>();

        public boolean hasChanges() {
            return !added.isEmpty() || !updated.isEmpty();
        }
    }

    private SyncMerge() {
    }

    /**
     * @param party    The party's characters; updated characters are changed in place, the list
     *                 itself is not changed.
     * @param applied  Per player uid, the sentAt of the sheet last applied; updated by this call.
     * @param received Per player uid, their latest sheet.
     * @return What changed.
     */
    public static Result merge(List<CharacterModel> party, Map<String, Long> applied,
                               Map<String, PlayerSheet> received) {
        Result result = new Result();
        for (Map.Entry<String, PlayerSheet> entry : received.entrySet()) {
            String uid = entry.getKey();
            PlayerSheet sheet = entry.getValue();
            if (uid == null || sheet == null) continue;
            Long last = applied.get(uid);
            if (last != null && sheet.sentAt <= last) continue;

            CharacterModel character = find(party, uid);
            if (character == null) {
                character = new CharacterModel();
                character.setSyncId(uid);
                sheet.applyTo(character);
                result.added.add(character);
            } else {
                sheet.applyTo(character);
                result.updated.add(character);
            }
            applied.put(uid, sheet.sentAt);
        }
        return result;
    }

    /** @return The first character in the party sent by this player, or null. */
    static CharacterModel find(List<CharacterModel> party, String uid) {
        for (CharacterModel c : party) {
            if (uid.equals(c.getSyncId())) return c;
        }
        return null;
    }
}
