package com.rekijan.initiativetracker.feature.sync;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.core.data.KeyValueStore;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Saves the Table sync state on the device, in the app's SharedPreferences:
 * <ul>
 *     <li>GM side: the code of the hosted session, the party it sends to, and per player the
 *     sentAt of the send the GM last approved or refused.</li>
 *     <li>Player side, per player character: the code of the joined session; when it was last
 *     sent and last updated from the GM; the GM's version it last got (the merge base); what it
 *     last sent, until answered; and which answer it has seen. The character itself is kept by
 *     PlayerBook.</li>
 * </ul>
 * Plain Java on top of {@link KeyValueStore}, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncStore {

    static final String KEY_HOST_CODE = "sync_host_code";
    static final String KEY_HOST_PARTY = "sync_host_party_id";
    static final String KEY_HOST_APPLIED = "sync_host_applied";
    static final String KEY_PLAYER_CODE_PREFIX = "sync_player_code_";
    static final String KEY_PLAYER_SENT_AT_PREFIX = "sync_player_sent_at_";
    static final String KEY_PLAYER_SYNCED_AT_PREFIX = "sync_player_synced_at_";
    static final String KEY_PLAYER_BASE_PREFIX = "sync_player_base_";
    static final String KEY_PLAYER_SENT_PREFIX = "sync_player_sent_";
    static final String KEY_PLAYER_ANSWERED_PREFIX = "sync_player_answered_";

    private static final Gson GSON = new Gson();
    private static final Type APPLIED_TYPE = new TypeToken<HashMap<String, Long>>() {
    }.getType();

    private final KeyValueStore values;

    public SyncStore(KeyValueStore values) {
        this.values = values;
    }

    // ---------------------------------------------------------------- GM

    /** @return The hosted session's code, or null when not hosting. */
    public String getHostCode() {
        return values.getString(KEY_HOST_CODE, null);
    }

    /** @return The id of the party the hosted session sends to, or null when not hosting. */
    public String getHostPartyId() {
        return values.getString(KEY_HOST_PARTY, null);
    }

    /** Starts hosting: a fresh session has no applied sends yet. */
    public void startHosting(String code, String partyId) {
        values.edit()
                .putString(KEY_HOST_CODE, code)
                .putString(KEY_HOST_PARTY, partyId)
                .remove(KEY_HOST_APPLIED)
                .apply();
    }

    public void stopHosting() {
        values.edit()
                .remove(KEY_HOST_CODE)
                .remove(KEY_HOST_PARTY)
                .remove(KEY_HOST_APPLIED)
                .apply();
    }

    /** @return A modifiable copy of the applied sends; empty when none or unreadable. */
    public Map<String, Long> loadApplied() {
        String json = values.getString(KEY_HOST_APPLIED, null);
        if (json == null) return new HashMap<>();
        try {
            Map<String, Long> map = GSON.fromJson(json, APPLIED_TYPE);
            return map != null ? new HashMap<>(map) : new HashMap<>();
        } catch (JsonParseException e) {
            return new HashMap<>();
        }
    }

    public void saveApplied(Map<String, Long> applied) {
        values.edit().putString(KEY_HOST_APPLIED, GSON.toJson(applied, APPLIED_TYPE)).apply();
    }

    // ---------------------------------------------------------------- Player (per character)

    /** @return The code of the session this player character joined, or null when not joined. */
    public String getPlayerCode(String characterId) {
        return values.getString(KEY_PLAYER_CODE_PREFIX + characterId, null);
    }

    /** Joins (code) or leaves (null); leaving also forgets when it was last sent and synced. */
    public void setPlayerCode(String characterId, String code) {
        if (code == null) {
            values.edit()
                    .remove(KEY_PLAYER_CODE_PREFIX + characterId)
                    .remove(KEY_PLAYER_SYNCED_AT_PREFIX + characterId)
                    .remove(KEY_PLAYER_BASE_PREFIX + characterId)
                    .remove(KEY_PLAYER_SENT_PREFIX + characterId)
                    .remove(KEY_PLAYER_ANSWERED_PREFIX + characterId)
                    .apply();
        } else {
            values.edit().putString(KEY_PLAYER_CODE_PREFIX + characterId, code).apply();
        }
    }

    /** @return When this character was last sent, in ms, or 0 when never. */
    public long getPlayerSentAt(String characterId) {
        return readLong(KEY_PLAYER_SENT_AT_PREFIX + characterId);
    }

    /**
     * Picks the sentAt for a new send: the clock, but always later than the previous send, so the
     * GM's app sees it as new even when the phone's clock was set back.
     *
     * @param now The current time in ms.
     * @return The sentAt to use; not saved until {@link #setPlayerSentAt}.
     */
    public long nextSentAt(String characterId, long now) {
        return Math.max(now, getPlayerSentAt(characterId) + 1);
    }

    public void setPlayerSentAt(String characterId, long sentAt) {
        values.edit().putString(KEY_PLAYER_SENT_AT_PREFIX + characterId, Long.toString(sentAt)).apply();
    }

    /** @return When the GM's copy was last pulled with Sync, in ms, or 0 when never. */
    public long getPlayerSyncedAt(String characterId) {
        return readLong(KEY_PLAYER_SYNCED_AT_PREFIX + characterId);
    }

    public void setPlayerSyncedAt(String characterId, long syncedAt) {
        values.edit().putString(KEY_PLAYER_SYNCED_AT_PREFIX + characterId, Long.toString(syncedAt)).apply();
    }

    /** Forgets everything about a deleted player character. */
    public void forgetPlayer(String characterId) {
        values.edit()
                .remove(KEY_PLAYER_CODE_PREFIX + characterId)
                .remove(KEY_PLAYER_SENT_AT_PREFIX + characterId)
                .remove(KEY_PLAYER_SYNCED_AT_PREFIX + characterId)
                .remove(KEY_PLAYER_BASE_PREFIX + characterId)
                .remove(KEY_PLAYER_SENT_PREFIX + characterId)
                .remove(KEY_PLAYER_ANSWERED_PREFIX + characterId)
                .apply();
    }

    /** @return The GM's version this character last got (the merge base), or null when none yet. */
    public PlayerSheet loadPlayerBase(String characterId) {
        return readSheet(KEY_PLAYER_BASE_PREFIX + characterId);
    }

    public void savePlayerBase(String characterId, PlayerSheet sheet) {
        values.edit().putString(KEY_PLAYER_BASE_PREFIX + characterId, GSON.toJson(sheet.cleaned())).apply();
    }

    /** @return What this character last sent, until the GM answered it; null otherwise. */
    public PlayerSheet loadPlayerSent(String characterId) {
        return readSheet(KEY_PLAYER_SENT_PREFIX + characterId);
    }

    public void savePlayerSent(String characterId, PlayerSheet sheet) {
        values.edit().putString(KEY_PLAYER_SENT_PREFIX + characterId, GSON.toJson(sheet.cleaned())).apply();
    }

    public void clearPlayerSent(String characterId) {
        values.edit().remove(KEY_PLAYER_SENT_PREFIX + characterId).apply();
    }

    /** @return The sentAt of the last send whose answer from the GM this character has seen; 0 when none. */
    public long getPlayerAnswered(String characterId) {
        return readLong(KEY_PLAYER_ANSWERED_PREFIX + characterId);
    }

    public void setPlayerAnswered(String characterId, long sentAt) {
        values.edit().putString(KEY_PLAYER_ANSWERED_PREFIX + characterId, Long.toString(sentAt)).apply();
    }

    private PlayerSheet readSheet(String key) {
        String json = values.getString(key, null);
        if (json == null) return null;
        try {
            PlayerSheet sheet = GSON.fromJson(json, PlayerSheet.class);
            return sheet != null ? sheet.cleaned() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    private long readLong(String key) {
        String text = values.getString(key, null);
        try {
            return text != null ? Long.parseLong(text) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
