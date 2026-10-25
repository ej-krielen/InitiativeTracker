package com.rekijan.initiativetracker.feature.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs Table sync for the whole app, so it keeps working whichever screen is open. Follows
 * Secret Roller's SessionManager.
 * <p>
 * GM side: starts a session tied to the active party, listens to the players' sheets and merges
 * new sends into that party ({@link SyncMerge}) while it is the active one; after switching back
 * to it, the latest sheets are merged again. The session survives an app restart. Firestore drops
 * a listener after an error; it is attached again after a delay that doubles up to five minutes,
 * and a session that turns out to be gone is ended.
 * <p>
 * Player side: joins a session by code and sends the character form.
 * <p>
 * Signing in (anonymously) only happens when the user starts or joins a session, so users who
 * never use Table sync never get a Firebase account. Firebase itself (App Check, Firestore) is only set up
 * then too, or at startup when a session is still running, so a GM who plays standalone never
 * talks to Firebase at all.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final long RETRY_MIN_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long RETRY_MAX_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long SIGN_IN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15);

    /** The app side Table sync needs: the active party and saving it. */
    public interface Host {
        String getActivePartyId();

        Encounter getEncounter();

        void saveEncounter();
    }

    /** Told when anything Table sync shows has changed: state, players, a merge. */
    public interface StateListener {
        void onSyncStateChanged();
    }

    /** Told about characters a merge added, to show who joined. */
    public interface JoinListener {
        void onPlayersAdded(List<String> names);
    }

    /** Result of an action the user waits for. */
    public interface Callback {
        void onSuccess();

        /** The session code doesn't exist (any more). */
        void onNotFound();

        void onFailure(Exception e);
    }

    private final Host host;
    private final SyncRepository repository;
    private final SyncStore store;
    private final Runnable firebaseSetup;
    private boolean firebaseReady;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Separate from mainHandler, whose retries are cancelled all at once
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final List<StateListener> stateListeners = new ArrayList<>();
    private JoinListener joinListener;
    private ListenerRegistration playersRegistration;
    private Map<String, PlayerSheet> lastSheets = new LinkedHashMap<>();
    private long retryDelayMs = RETRY_MIN_MS;

    /**
     * @param firebaseSetup Sets up Firebase (App Check); run once, right before the first Firebase call.
     */
    public SyncManager(Host host, SyncRepository repository, SyncStore store, Runnable firebaseSetup) {
        this.firebaseSetup = firebaseSetup;
        this.host = host;
        this.repository = repository;
        this.store = store;
    }

    public void addStateListener(StateListener listener) {
        if (!stateListeners.contains(listener)) stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public void setJoinListener(@Nullable JoinListener listener) {
        joinListener = listener;
    }

    // ---------------------------------------------------------------- GM

    @Nullable
    public String getHostCode() {
        return store.getHostCode();
    }

    @Nullable
    public String getHostPartyId() {
        return store.getHostPartyId();
    }

    /** @return The latest sheets per player uid, in the order Firestore gave them. Read-only. */
    public Map<String, PlayerSheet> getPlayers() {
        return Collections.unmodifiableMap(lastSheets);
    }

    /** Picks up a hosted session after an app start; ends it when the server says it is gone. */
    public void resumeOnStartup() {
        String code = store.getHostCode();
        if (code == null) return;
        ensureFirebase();
        attachListener(code);
        verify(code, false);
    }

    /** Starts hosting a new session for the active party. */
    public void startHosting(@NonNull Callback callback) {
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.createSession(uid, new SyncRepository.CreateCallback() {
                    @Override
                    public void onCreated(String code) {
                        store.startHosting(code, host.getActivePartyId());
                        lastSheets = new LinkedHashMap<>();
                        attachListener(code);
                        notifyStateChanged();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Stops hosting on this device right away, then deletes the session online so the code stops
     * working and the players' sheets are gone. Characters already in the party stay.
     *
     * @param callback Told whether the online delete worked; it expires anyway when it didn't.
     */
    public void stopHosting(@NonNull SyncRepository.CompletionCallback callback) {
        String code = store.getHostCode();
        endHostingLocally();
        if (code == null) {
            callback.onSuccess();
            return;
        }
        repository.deleteSession(code, callback);
    }

    /** The active party changed: merge the latest sheets when it is the hosted party again. */
    public void onActivePartyChanged() {
        mergeIntoActiveParty();
    }

    /** A party was deleted: when it was the hosted one, the session ends. */
    public void onPartyDeleted(String partyId) {
        if (partyId.equals(store.getHostPartyId())) {
            stopHosting(new SyncRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    // Nothing to show: the user just deleted the party
                }

                @Override
                public void onFailure(Exception e) {
                    // Logged by the repository; the session expires on its own
                }
            });
        }
    }

    // ---------------------------------------------------------------- Player

    @Nullable
    public String getPlayerCode() {
        return store.getPlayerCode();
    }

    public PlayerSheet loadPlayerSheet() {
        return store.loadPlayerSheet();
    }

    public void savePlayerSheet(PlayerSheet sheet) {
        store.savePlayerSheet(sheet);
    }

    /** @return When the form was last sent, in ms, or 0 when never. */
    public long getPlayerSentAt() {
        return store.getPlayerSentAt();
    }

    /**
     * Joins a session after checking on the server that it exists.
     *
     * @param code A normalized, valid code (see {@link SessionCodes}).
     */
    public void join(@NonNull String code, @NonNull Callback callback) {
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
                    @Override
                    public void onResult(@Nullable Timestamp expiresAt) {
                        if (expiresAt == null) {
                            callback.onNotFound();
                            return;
                        }
                        store.setPlayerCode(code);
                        notifyStateChanged();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Saves the form and sends it to the joined session. When the session is gone, the player is
     * taken out of it and told so.
     */
    public void send(@NonNull PlayerSheet sheet, @NonNull Callback callback) {
        String code = store.getPlayerCode();
        store.savePlayerSheet(sheet);
        if (code == null) {
            callback.onNotFound();
            return;
        }
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
                    @Override
                    public void onResult(@Nullable Timestamp expiresAt) {
                        if (expiresAt == null) {
                            store.setPlayerCode(null);
                            notifyStateChanged();
                            callback.onNotFound();
                            return;
                        }
                        PlayerSheet toSend = sheet.cleaned();
                        toSend.sentAt = store.nextSentAt(System.currentTimeMillis());
                        repository.savePlayer(code, uid, toSend, expiresAt, new SyncRepository.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                store.setPlayerSentAt(toSend.sentAt);
                                notifyStateChanged();
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /** Leaves the joined session; the sheet sent to it is removed (best effort). */
    public void leave() {
        String code = store.getPlayerCode();
        if (code != null) {
            ensureFirebase();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) repository.deletePlayer(code, user.getUid());
        }
        store.setPlayerCode(null);
        notifyStateChanged();
    }

    // ---------------------------------------------------------------- Private

    private interface SignInCallback {
        void onSignedIn(String uid);

        void onFailure(Exception e);
    }

    private void signIn(SignInCallback callback) {
        ensureFirebase();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            callback.onSignedIn(user.getUid());
            return;
        }
        // Without a connection sign-in waits forever, so give up after a while; only one answer counts
        boolean[] answered = {false};
        Runnable timeout = () -> {
            if (answered[0]) return;
            answered[0] = true;
            Log.w(TAG, "Anonymous sign-in timed out");
            callback.onFailure(new TimeoutException("Sign-in timed out"));
        };
        timeoutHandler.postDelayed(timeout, SIGN_IN_TIMEOUT_MS);
        auth.signInAnonymously().addOnCompleteListener(task -> {
            timeoutHandler.removeCallbacks(timeout);
            if (answered[0]) return;
            answered[0] = true;
            FirebaseUser signedIn = task.isSuccessful() ? task.getResult().getUser() : null;
            if (signedIn != null) {
                callback.onSignedIn(signedIn.getUid());
            } else {
                Exception e = task.getException() != null ? task.getException()
                        : new IllegalStateException("No user after sign-in");
                Log.w(TAG, "Anonymous sign-in failed", e);
                callback.onFailure(e);
            }
        });
    }

    private void ensureFirebase() {
        if (firebaseReady) return;
        firebaseSetup.run();
        firebaseReady = true;
    }

    private void attachListener(String code) {
        cancelRetry();
        detachListener();
        playersRegistration = repository.listenToPlayers(code, new SyncRepository.PlayersListener() {
            @Override
            public void onPlayers(Map<String, PlayerSheet> sheets) {
                if (!code.equals(store.getHostCode())) return;
                retryDelayMs = RETRY_MIN_MS;
                lastSheets = new LinkedHashMap<>(sheets);
                mergeIntoActiveParty();
                notifyStateChanged();
            }

            @Override
            public void onError(FirebaseFirestoreException error) {
                Log.w(TAG, "Players listener error: " + error.getCode(), error);
                // Firestore has removed the listener already
                playersRegistration = null;
                scheduleRetry(code);
            }
        });
    }

    /** Merges the latest sheets into the active party, only when it is the hosted party. */
    private void mergeIntoActiveParty() {
        String partyId = store.getHostPartyId();
        if (partyId == null || !partyId.equals(host.getActivePartyId()) || lastSheets.isEmpty()) return;
        Encounter encounter = host.getEncounter();
        Map<String, Long> applied = store.loadApplied();
        SyncMerge.Result result = SyncMerge.merge(encounter.getCharacters(), applied, lastSheets);
        if (!result.hasChanges()) return;
        if (!result.added.isEmpty()) {
            encounter.addAll(result.added);
        } else {
            encounter.notifyChanged();
        }
        store.saveApplied(applied);
        host.saveEncounter();
        if (!result.added.isEmpty() && joinListener != null) {
            List<String> names = new ArrayList<>();
            for (CharacterModel c : result.added) names.add(c.getCharacterName());
            joinListener.onPlayersAdded(names);
        }
    }

    /**
     * Checks that the hosted session still exists: gone ends it, there renews its expiry (and
     * attaches the listener again when asked). A failed check keeps the session.
     */
    private void verify(String code, boolean reattach) {
        repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
            @Override
            public void onResult(@Nullable Timestamp expiresAt) {
                if (!code.equals(store.getHostCode())) return;
                if (expiresAt == null) {
                    Log.i(TAG, "Hosted session is gone: " + code);
                    endHostingLocally();
                    return;
                }
                if (reattach) attachListener(code);
                repository.extendSessionIfNeeded(code);
            }

            @Override
            public void onFailure(Exception e) {
                if (reattach) scheduleRetry(code);
            }
        });
    }

    private void endHostingLocally() {
        cancelRetry();
        detachListener();
        store.stopHosting();
        lastSheets = new LinkedHashMap<>();
        retryDelayMs = RETRY_MIN_MS;
        notifyStateChanged();
    }

    private void scheduleRetry(String code) {
        long delay = retryDelayMs;
        retryDelayMs = Math.min(retryDelayMs * 2, RETRY_MAX_MS);
        cancelRetry();
        mainHandler.postDelayed(() -> {
            if (code.equals(store.getHostCode())) verify(code, true);
        }, delay);
    }

    private void cancelRetry() {
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void detachListener() {
        if (playersRegistration != null) {
            playersRegistration.remove();
            playersRegistration = null;
        }
    }

    private void notifyStateChanged() {
        for (StateListener l : new ArrayList<>(stateListeners)) {
            l.onSyncStateChanged();
        }
    }
}
