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
 *     sentAt of the sheet last applied (see {@link SyncMerge}).</li>
 *     <li>Player side: the code of the joined session, the character form, and when it was last
 *     sent.</li>
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
    static final String KEY_PLAYER_CODE = "sync_player_code";
    static final String KEY_PLAYER_SHEET = "sync_player_sheet";
    static final String KEY_PLAYER_SENT_AT = "sync_player_sent_at";

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

    // ---------------------------------------------------------------- Player

    /** @return The joined session's code, or null when not joined. */
    public String getPlayerCode() {
        return values.getString(KEY_PLAYER_CODE, null);
    }

    public void setPlayerCode(String code) {
        if (code == null) {
            values.edit().remove(KEY_PLAYER_CODE).apply();
        } else {
            values.edit().putString(KEY_PLAYER_CODE, code).apply();
        }
    }

    /** @return The saved character form, or an empty one. */
    public PlayerSheet loadPlayerSheet() {
        String json = values.getString(KEY_PLAYER_SHEET, null);
        if (json != null) {
            try {
                PlayerSheet sheet = GSON.fromJson(json, PlayerSheet.class);
                if (sheet != null) return sheet.cleaned();
            } catch (JsonParseException e) {
                // Unreadable: start with an empty form
            }
        }
        return new PlayerSheet();
    }

    public void savePlayerSheet(PlayerSheet sheet) {
        values.edit().putString(KEY_PLAYER_SHEET, GSON.toJson(sheet.cleaned())).apply();
    }

    /** @return When the form was last sent, in ms, or 0 when never. */
    public long getPlayerSentAt() {
        String text = values.getString(KEY_PLAYER_SENT_AT, null);
        try {
            return text != null ? Long.parseLong(text) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Picks the sentAt for a new send: the clock, but always later than the previous send, so the
     * GM's app sees it as new even when the phone's clock was set back.
     *
     * @param now The current time in ms.
     * @return The sentAt to use; not saved until {@link #setPlayerSentAt}.
     */
    public long nextSentAt(long now) {
        return Math.max(now, getPlayerSentAt() + 1);
    }

    public void setPlayerSentAt(long sentAt) {
        values.edit().putString(KEY_PLAYER_SENT_AT, Long.toString(sentAt)).apply();
    }
}
