package com.rekijan.initiativetracker.feature.player;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.CharacterSheet;
import com.rekijan.initiativetracker.feature.sync.SessionCodes;
import com.rekijan.initiativetracker.feature.sync.SyncFragment;
import com.rekijan.initiativetracker.feature.sync.SyncManager;

import java.util.Date;

/**
 * A player's own character, like Secret Roller's character screen: the session card on top, the
 * sheet with the Stats and Conditions tabs ({@link CharacterSheet}), and Send to GM.
 * <p>
 * Not joined, the card has the session code and Join. Joined, it shows a status light, when the
 * character was last sent and synced, Sync (pull the GM's hp, max hp and conditions for this
 * character) and Leave. The sheet also works without a session, to track your own character.
 * Every edit is saved on the phone straight away.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerFragment extends Fragment implements SyncManager.StateListener {

    private static final String ARG_ID = "PLAYER_ID";
    private static final String STATE_TAB = "sheet_tab";

    private InitiativeTrackerApp app;
    private SyncManager sync;
    private String characterId;
    private CharacterModel character;
    private CharacterSheet sheet;
    private boolean busy;

    public static PlayerFragment newInstance(String characterId) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, characterId);
        fragment.setArguments(args);
        return fragment;
    }

    /** @return The id of the player character shown here. */
    public String getCharacterId() {
        return requireArguments().getString(ARG_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (InitiativeTrackerApp) requireActivity().getApplication();
        sync = app.getSync();
        characterId = getCharacterId();
        character = app.getPlayers().load(characterId);

        sheet = new CharacterSheet(view.findViewById(R.id.player_sheet), true, this::onSheetChanged);
        sheet.bind(character);
        if (savedInstanceState != null) {
            sheet.selectTab(savedInstanceState.getInt(STATE_TAB, CharacterSheet.TAB_STATS));
        }

        view.findViewById(R.id.player_join_button).setOnClickListener(v -> join());
        view.findViewById(R.id.player_sync_button).setOnClickListener(v -> pull());
        view.findViewById(R.id.player_leave_button).setOnClickListener(v -> sync.leave(characterId));
        view.findViewById(R.id.player_send_button).setOnClickListener(v -> send());
        EditText codeText = view.findViewById(R.id.player_code_editText);
        codeText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            join();
            return true;
        });

        sync.addStateListener(this);
        renderSession();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sheet != null) outState.putInt(STATE_TAB, sheet.getSelectedTab());
    }

    @Override
    public void onDestroyView() {
        sync.removeStateListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSyncStateChanged() {
        if (getView() != null) renderSession();
    }

    private void onSheetChanged() {
        app.getPlayers().save(characterId, character);
    }

    private void renderSession() {
        View view = requireView();
        String code = sync.getPlayerCode(characterId);
        boolean joined = code != null;
        view.findViewById(R.id.player_join_row).setVisibility(joined ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.player_joined_row).setVisibility(joined ? View.VISIBLE : View.GONE);
        int lampColor = joined ? ContextCompat.getColor(requireContext(), R.color.rk_status_on)
                : MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline);
        view.findViewById(R.id.player_status_lamp).setBackgroundTintList(ColorStateList.valueOf(lampColor));
        if (!joined) return;

        StringBuilder text = new StringBuilder(getString(R.string.sync_joined, code));
        long sentAt = sync.getPlayerSentAt(characterId);
        long syncedAt = sync.getPlayerSyncedAt(characterId);
        java.text.DateFormat time = DateFormat.getTimeFormat(requireContext());
        if (sentAt > 0 || syncedAt > 0) text.append('\n');
        if (sentAt > 0) text.append(getString(R.string.player_sent_at, time.format(new Date(sentAt))));
        if (sentAt > 0 && syncedAt > 0) text.append(" · ");
        if (syncedAt > 0) text.append(getString(R.string.player_synced_at, time.format(new Date(syncedAt))));
        ((TextView) view.findViewById(R.id.player_joined_textView)).setText(text);
    }

    private void join() {
        if (busy) return;
        View view = requireView();
        TextInputLayout codeLayout = view.findViewById(R.id.player_code_layout);
        EditText codeText = view.findViewById(R.id.player_code_editText);
        String code = SessionCodes.normalize(codeText.getText().toString());
        if (!SessionCodes.isValid(code)) {
            codeLayout.setError(getString(R.string.sync_code_invalid, SessionCodes.LENGTH));
            return;
        }
        codeLayout.setError(null);
        busy = true;
        sync.join(characterId, code, new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                busy = false;
                if (getView() != null) codeText.setText("");
            }

            @Override
            public void onNotFound() {
                busy = false;
                if (getView() != null) codeLayout.setError(getString(R.string.sync_code_not_found));
            }

            @Override
            public void onFailure(Exception e) {
                busy = false;
                toast(SyncFragment.errorMessage(e));
            }
        });
    }

    private void send() {
        if (busy) return;
        if (sync.getPlayerCode(characterId) == null) {
            toast(R.string.sync_join_first);
            return;
        }
        if (character.getCharacterName().trim().isEmpty()) {
            sheet.selectTab(CharacterSheet.TAB_STATS);
            toast(R.string.sync_name_missing);
            return;
        }
        busy = true;
        sync.send(characterId, character, new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                busy = false;
                toast(R.string.sync_sent);
            }

            @Override
            public void onNotFound() {
                busy = false;
                toast(R.string.sync_session_gone);
            }

            @Override
            public void onFailure(Exception e) {
                busy = false;
                toast(SyncFragment.errorMessage(e));
            }
        });
    }

    /** Sync: the GM leads, so their hp, max hp and conditions replace the ones on this sheet. */
    private void pull() {
        if (busy) return;
        busy = true;
        sync.pull(characterId, character, new SyncManager.PullCallback() {
            @Override
            public void onPulled() {
                busy = false;
                app.getPlayers().save(characterId, character);
                if (getView() != null) sheet.bind(character);
                toast(R.string.player_synced);
            }

            @Override
            public void onNoCopy() {
                busy = false;
                toast(R.string.player_no_copy);
            }

            @Override
            public void onNotFound() {
                busy = false;
                toast(R.string.sync_session_gone);
            }

            @Override
            public void onFailure(Exception e) {
                busy = false;
                toast(SyncFragment.errorMessage(e));
            }
        });
    }

    private void toast(@StringRes int message) {
        Context context = getContext();
        if (context != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
