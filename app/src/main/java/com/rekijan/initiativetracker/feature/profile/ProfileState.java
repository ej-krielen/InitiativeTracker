package com.rekijan.initiativetracker.feature.profile;

import com.rekijan.initiativetracker.core.data.KeyValueStore;

/**
 * Remembers which profile was opened last, so the app opens it again on the next start: a party
 * (the GM's encounter; which party is kept by PartyBook) or one of the player's characters. A
 * fresh install and the 1.x update open the party, as before. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class ProfileState {

    static final String KEY_KIND = "active_profile_kind";
    static final String KEY_PLAYER_ID = "active_player_id";
    static final String KIND_PARTY = "party";
    static final String KIND_PLAYER = "player";

    private final KeyValueStore values;

    public ProfileState(KeyValueStore values) {
        this.values = values;
    }

    /** @return True when a player character was opened last; false for a party. */
    public boolean isPlayer() {
        return KIND_PLAYER.equals(values.getString(KEY_KIND, KIND_PARTY))
                && values.getString(KEY_PLAYER_ID, null) != null;
    }

    /** @return The id of the player character opened last, or null when a party was. */
    public String getPlayerId() {
        return isPlayer() ? values.getString(KEY_PLAYER_ID, null) : null;
    }

    /** The party (PartyBook's active one) is open now. */
    public void openParty() {
        values.edit().putString(KEY_KIND, KIND_PARTY).remove(KEY_PLAYER_ID).apply();
    }

    public void openPlayer(String playerId) {
        values.edit().putString(KEY_KIND, KIND_PLAYER).putString(KEY_PLAYER_ID, playerId).apply();
    }
}
