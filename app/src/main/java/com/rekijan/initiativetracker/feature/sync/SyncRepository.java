package com.rekijan.initiativetracker.feature.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * All Firestore access for Table sync, following Secret Roller's SessionRepository. A session is
 * sessions/{code} (owner gmUid, createdAt, expiresAt); each player's sheet is
 * sessions/{code}/players/{uid}. The rules in firestore.rules decide who may do what.
 * <p>
 * A session lives until {@link #SESSION_RETENTION_DAYS} days after the GM last used it: the GM's
 * app renews the expiry of the session and its players when it opens the session. Expired
 * sessions are removed by {@link #cleanupExpiredSessions}, and the rules refuse writes to them.
 * <p>
 * Actions the user waits for report failure after at most {@link #TIMEOUT_SECONDS} seconds,
 * because a Firestore write without a connection waits instead of failing. Background work
 * (renewing, cleanup) only logs.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncRepository {

    private static final String TAG = "SyncRepository";
    private static final String COLLECTION_SESSIONS = "sessions";
    private static final String COLLECTION_PLAYERS = "players";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_GM_UID = "gmUid";
    private static final String KEY_EXPIRES_AT = "expiresAt";
    /** Keep in sync with the 33-day cap in firestore.rules (30 plus clock margin) and the privacy policy. */
    private static final long SESSION_RETENTION_DAYS = 30;
    /** The expiry is only renewed when that gains at least a day, to avoid needless writes. */
    private static final long MIN_EXTENSION_GAIN_DAYS = 1;
    private static final long TIMEOUT_SECONDS = 15;

    // Created on first use, so the app never touches Firestore unless Table sync is used
    private FirebaseFirestore db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    /** An online action that only succeeds or fails. */
    public interface CompletionCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    /** Result of creating a session. */
    public interface CreateCallback {
        void onCreated(String code);

        void onFailure(Exception e);
    }

    /** Result of looking up a session's expiry. */
    public interface ExpiryCallback {
        /** @param expiresAt Null when the session does not exist or has expired. */
        void onResult(@Nullable Timestamp expiresAt);

        void onFailure(Exception e);
    }

    /** Live updates of the players in a session. */
    public interface PlayersListener {
        /** @param sheets Per player uid, their latest (cleaned) sheet. */
        void onPlayers(Map<String, PlayerSheet> sheets);

        void onError(FirebaseFirestoreException error);
    }

    /** Creates a session with a new code, owned by gmUid, and cleans up this GM's expired ones. */
    public void createSession(@NonNull String gmUid, @NonNull CreateCallback callback) {
        String code = SessionCodes.generate(random);
        Map<String, Object> data = new HashMap<>();
        data.put(KEY_GM_UID, gmUid);
        data.put(KEY_CREATED_AT, FieldValue.serverTimestamp());
        data.put(KEY_EXPIRES_AT, newExpiry());
        withTimeout(sessionRef(code).set(data), "createSession " + code)
                .addOnSuccessListener(unused -> {
                    callback.onCreated(code);
                    cleanupExpiredSessions(gmUid);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createSession failed for " + code, e);
                    callback.onFailure(e);
                });
    }

    /** Looks up when a session expires, from the server; null when it is gone or expired. */
    public void fetchExpiry(@NonNull String code, @NonNull ExpiryCallback callback) {
        withTimeout(sessionRef(code).get(Source.SERVER), "fetchExpiry " + code)
                .addOnSuccessListener(doc -> callback.onResult(
                        doc.exists() && !isExpired(doc) ? doc.getTimestamp(KEY_EXPIRES_AT) : null))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "fetchExpiry failed for " + code, e);
                    callback.onFailure(e);
                });
    }

    /** Writes the player's sheet to sessions/{code}/players/{uid}; it expires with the session. */
    public void savePlayer(@NonNull String code, @NonNull String uid, @NonNull PlayerSheet sheet,
                           @NonNull Timestamp expiresAt, @NonNull CompletionCallback callback) {
        Map<String, Object> data = sheet.toMap();
        data.put(KEY_EXPIRES_AT, expiresAt);
        withTimeout(playerRef(code, uid).set(data), "savePlayer " + code)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "savePlayer failed for " + code, e);
                    callback.onFailure(e);
                });
    }

    /** Removes the player's own sheet when they leave. Best effort: it expires anyway. */
    public void deletePlayer(@NonNull String code, @NonNull String uid) {
        playerRef(code, uid).delete().addOnFailureListener(e ->
                Log.w(TAG, "deletePlayer failed for " + code, e));
    }

    /**
     * Listens to all players' sheets in a session. The caller removes the returned registration
     * when done. Sheets without a name are left out.
     */
    @NonNull
    public ListenerRegistration listenToPlayers(@NonNull String code, @NonNull PlayersListener listener) {
        return sessionRef(code).collection(COLLECTION_PLAYERS).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot == null) return;
            Map<String, PlayerSheet> sheets = new LinkedHashMap<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                PlayerSheet sheet = PlayerSheet.fromMap(doc.getData());
                if (sheet != null) sheets.put(doc.getId(), sheet);
            }
            listener.onPlayers(sheets);
        });
    }

    /**
     * Deletes a session: first its players (one by one, the rules allow that only while the
     * session exists), then the session itself.
     */
    public void deleteSession(@NonNull String code, @NonNull CompletionCallback callback) {
        DocumentReference ref = sessionRef(code);
        withTimeout(ref.collection(COLLECTION_PLAYERS).get(Source.SERVER), "deleteSession read " + code)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) return Tasks.<Void>forException(failure(task));
                    List<Task<Void>> deletes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        deletes.add(doc.getReference().delete());
                    }
                    return withTimeout(Tasks.whenAll(deletes), "deleteSession players " + code);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) return task;
                    return withTimeout(ref.delete(), "deleteSession " + code);
                })
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "deleteSession failed for " + code, e);
                    callback.onFailure(e);
                });
    }

    /**
     * Renews the expiry of the session and then of its players, when that gains at least a day.
     * Only the owning GM is allowed to; failures are only logged.
     */
    public void extendSessionIfNeeded(@NonNull String code) {
        DocumentReference ref = sessionRef(code);
        ref.get(Source.SERVER).addOnSuccessListener(doc -> {
            Timestamp current = doc.getTimestamp(KEY_EXPIRES_AT);
            long now = System.currentTimeMillis();
            long newMillis = now + TimeUnit.DAYS.toMillis(SESSION_RETENTION_DAYS);
            if (!doc.exists() || current == null || current.toDate().getTime() <= now) return;
            if (newMillis - current.toDate().getTime() < TimeUnit.DAYS.toMillis(MIN_EXTENSION_GAIN_DAYS)) return;
            Timestamp newExpiry = new Timestamp(new Date(newMillis));
            ref.update(KEY_EXPIRES_AT, newExpiry)
                    .addOnSuccessListener(unused -> extendPlayers(ref, newExpiry))
                    .addOnFailureListener(e -> Log.w(TAG, "extend failed for " + code, e));
        }).addOnFailureListener(e -> Log.w(TAG, "extend: read failed for " + code, e));
    }

    /** Deletes this GM's expired sessions with their players. Best effort, only logs. */
    public void cleanupExpiredSessions(@NonNull String gmUid) {
        long now = System.currentTimeMillis();
        db().collection(COLLECTION_SESSIONS).whereEqualTo(KEY_GM_UID, gmUid).get()
                .addOnSuccessListener(sessions -> {
                    for (QueryDocumentSnapshot doc : sessions) {
                        Timestamp expiresAt = doc.getTimestamp(KEY_EXPIRES_AT);
                        if (expiresAt != null && expiresAt.toDate().getTime() < now) {
                            deleteSession(doc.getId(), new CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    // Background cleanup: nothing to show
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // Already logged by deleteSession
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "cleanup: query failed", e));
    }

    private void extendPlayers(DocumentReference ref, Timestamp newExpiry) {
        ref.collection(COLLECTION_PLAYERS).get(Source.SERVER).addOnSuccessListener(players -> {
            for (QueryDocumentSnapshot doc : players) {
                doc.getReference().update(KEY_EXPIRES_AT, newExpiry).addOnFailureListener(e ->
                        Log.w(TAG, "extend player failed: " + doc.getId(), e));
            }
        }).addOnFailureListener(e -> Log.w(TAG, "extend players: read failed", e));
    }

    private static Exception failure(Task<?> task) {
        Exception e = task.getException();
        return e != null ? e : new Exception("Firestore task failed");
    }

    private FirebaseFirestore db() {
        if (db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    private DocumentReference sessionRef(String code) {
        return db().collection(COLLECTION_SESSIONS).document(code);
    }

    private DocumentReference playerRef(String code, String uid) {
        return sessionRef(code).collection(COLLECTION_PLAYERS).document(uid);
    }

    /** Past its expiry a session counts as gone, like in the rules. */
    private static boolean isExpired(DocumentSnapshot doc) {
        Timestamp expiresAt = doc.getTimestamp(KEY_EXPIRES_AT);
        return expiresAt != null && expiresAt.toDate().getTime() < System.currentTimeMillis();
    }

    private static Timestamp newExpiry() {
        return new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(SESSION_RETENTION_DAYS)));
    }

    /**
     * Fails the returned task when the given one hasn't finished in {@link #TIMEOUT_SECONDS}. The
     * Firestore operation itself keeps going; a session written late just expires unused.
     */
    private <T> Task<T> withTimeout(Task<T> task, String what) {
        TaskCompletionSource<T> source = new TaskCompletionSource<>();
        Runnable timeout = () -> source.trySetException(new TimeoutException(what + " timed out"));
        mainHandler.postDelayed(timeout, TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        task.addOnCompleteListener(t -> {
            mainHandler.removeCallbacks(timeout);
            if (t.isSuccessful()) {
                source.trySetResult(t.getResult());
            } else {
                Exception e = t.getException();
                source.trySetException(e != null ? e : new Exception(what + " failed"));
            }
        });
        return source.getTask();
    }
}
