package com.rekijan.initiativetracker.feature.player;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.CharacterSheet;
import com.rekijan.initiativetracker.feature.sync.SessionCodes;
import com.rekijan.initiativetracker.feature.sync.SyncDiff;
import com.rekijan.initiativetracker.feature.sync.SyncFragment;
import com.rekijan.initiativetracker.feature.sync.SyncManager;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A player's own character, like Secret Roller's character screen: the session card on top, the
 * sheet with the Stats and Conditions tabs ({@link CharacterSheet}), and Send to GM.
 * <p>
 * Not joined, the card has the session code and Join, which also sends the sheet. Joined, the GM
 * is the final arbiter: Send asks the GM for the changes, and while this screen is open the GM's version comes in by itself
 * (their answer, damage, conditions). The card shows the state with a light: gold when there are
 * changes not sent yet (marked on the fields), amber while waiting for the GM, green when up to
 * date. The sheet also works without a session. Every edit is saved on the phone straight away.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PlayerFragment extends Fragment implements SyncManager.StateListener, SyncManager.PlayerUpdateListener {

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
        view.findViewById(R.id.player_leave_button).setOnClickListener(v -> askLeave());
        view.findViewById(R.id.player_send_button).setOnClickListener(v -> send());
        EditText codeText = view.findViewById(R.id.player_code_editText);
        codeText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            join();
            return true;
        });
        render();
    }

    @Override
    public void onStart() {
        super.onStart();
        sync.addStateListener(this);
        sync.setPlayerUpdateListener(this);
        sync.watchPlayer(characterId);
        render();
    }

    @Override
    public void onStop() {
        sync.unwatchPlayer(characterId);
        sync.setPlayerUpdateListener(null);
        sync.removeStateListener(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sheet != null) outState.putInt(STATE_TAB, sheet.getSelectedTab());
    }

    @Override
    public void onSyncStateChanged() {
        if (getView() == null) return;
        // Just joined: start following the GM's version
        sync.watchPlayer(characterId);
        render();
    }

    @Override
    public void onPlayerUpdated(String id, SyncManager.Answer answer, int kept) {
        if (!id.equals(characterId) || getView() == null) return;
        character = app.getPlayers().load(characterId);
        sheet.bind(character);
        render();
        switch (answer) {
            case ACCEPTED:
                toast(getString(R.string.player_answer_accepted));
                break;
            case KEPT:
                toast(getResources().getQuantityString(R.plurals.player_answer_kept, kept, kept));
                break;
            case REJECTED:
                toast(getString(R.string.player_answer_rejected, character.getCharacterName()));
                break;
            case REMOVED:
                showNotice(getString(R.string.player_notice_removed_title),
                        getString(R.string.player_notice_removed, displayName()));
                break;
            case UNLINKED:
                showNotice(getString(R.string.player_notice_unlinked_title, displayName()),
                        getString(R.string.player_notice_unlinked, displayName()));
                break;
            case SESSION_ENDED:
                showNotice(getString(R.string.player_notice_session_ended_title),
                        getString(R.string.player_notice_session_ended, displayName()));
                break;
            default:
                break;
        }
    }

    /** The GM ended the link: a popup, since the player may not be looking at the screen. */
    private void showNotice(String title, String message) {
        Dialogs.builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    /** Leaving is easy to tap by mistake, and the GM keeps the character: ask first. */
    private void askLeave() {
        Dialogs.builder(requireContext())
                .setTitle(R.string.player_leave_title)
                .setMessage(getString(R.string.player_leave_message, displayName()))
                .setPositiveButton(R.string.sync_leave, (d, w) -> sync.leave(characterId))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private String displayName() {
        String name = character.getCharacterName();
        return name.trim().isEmpty() ? getString(R.string.empty_character_name) : name;
    }

    private void onSheetChanged() {
        app.getPlayers().save(characterId, character);
        render();
    }

    /** The session card, the Send button and the not-sent marks. */
    private void render() {
        View view = getView();
        if (view == null) return;
        String code = sync.getPlayerCode(characterId);
        boolean joined = code != null;
        view.findViewById(R.id.player_join_row).setVisibility(joined ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.player_joined_row).setVisibility(joined ? View.VISIBLE : View.GONE);
        Button send = view.findViewById(R.id.player_send_button);

        if (!joined) {
            setLamp(view, R.color.md_outline);
            sheet.markUnsent(new HashSet<>(), false);
            send.setEnabled(true);
            send.setText(R.string.sync_send);
            return;
        }

        boolean waiting = sync.isWaitingForGm(characterId);
        List<SyncDiff.Change> unsent = sync.getUnsentChanges(characterId, character);
        String state;
        if (waiting) {
            setLamp(view, R.color.rk_status_wait);
            state = getString(R.string.player_state_waiting);
        } else if (unsent == null) {
            setLamp(view, R.color.rk_pc_marker);
            state = getString(R.string.player_state_not_added);
        } else if (!unsent.isEmpty()) {
            setLamp(view, R.color.rk_pc_marker);
            state = getResources().getQuantityString(R.plurals.player_state_unsent, unsent.size(), unsent.size());
        } else {
            setLamp(view, R.color.rk_status_on);
            long syncedAt = sync.getPlayerSyncedAt(characterId);
            state = syncedAt > 0
                    ? getString(R.string.player_state_up_to_date_at, DateFormat.getTimeFormat(requireContext()).format(new Date(syncedAt)))
                    : getString(R.string.player_state_up_to_date);
        }
        ((TextView) view.findViewById(R.id.player_joined_textView))
                .setText(getString(R.string.sync_joined, code) + "\n" + state);

        // Marks only make sense once there is a GM's version to compare with
        Set<Integer> ids = new HashSet<>();
        boolean conditions = false;
        if (unsent != null) {
            for (SyncDiff.Change c : unsent) {
                if (c.field == SyncDiff.Field.CONDITIONS) {
                    conditions = true;
                } else {
                    ids.add(editTextOf(c.field));
                }
            }
        }
        sheet.markUnsent(ids, conditions);

        send.setEnabled(!waiting);
        if (waiting) {
            send.setText(R.string.player_send_waiting);
        } else if (unsent == null || unsent.isEmpty()) {
            send.setText(R.string.sync_send);
        } else {
            send.setText(getResources().getQuantityString(R.plurals.player_send_count, unsent.size(), unsent.size()));
        }
    }

    private void setLamp(View view, int colorRes) {
        view.findViewById(R.id.player_status_lamp).setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes)));
    }

    private static int editTextOf(SyncDiff.Field field) {
        switch (field) {
            case NAME: return R.id.sheet_name_editText;
            case INITIATIVE: return R.id.sheet_initiative_editText;
            case HP: return R.id.sheet_hp_editText;
            case INITIATIVE_BONUS: return R.id.initiative_bonus_editText;
            case MAX_HP: return R.id.max_hp_editText;
            case FAST_HEALING: return R.id.fast_healing_editText;
            case REGENERATION: return R.id.regeneration_editText;
            case AC: return R.id.ac_editText;
            case SAVES: return R.id.saves_editText;
            case MANEUVERS: return R.id.maneuvers_editText;
            case SKILLS: return R.id.skills_editText;
            case ATTACK_ROUTINE: return R.id.attack_routine_editText;
            default: return R.id.notes_editText;
        }
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
                if (getView() == null) return;
                codeText.setText("");
                // Joining syncs right away: a new player asks to join, a known one is linked again
                send();
            }

            @Override
            public void onNotFound() {
                busy = false;
                if (getView() != null) codeLayout.setError(getString(R.string.sync_code_not_found));
            }

            @Override
            public void onFailure(Exception e) {
                busy = false;
                toast(getString(SyncFragment.errorMessage(e)));
            }
        });
    }

    private void send() {
        if (busy) return;
        if (sync.getPlayerCode(characterId) == null) {
            toast(getString(R.string.sync_join_first));
            return;
        }
        if (character.getCharacterName().trim().isEmpty()) {
            sheet.selectTab(CharacterSheet.TAB_STATS);
            toast(getString(R.string.sync_name_missing));
            return;
        }
        busy = true;
        sync.send(characterId, character, new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                busy = false;
                toast(getString(R.string.sync_sent));
            }

            @Override
            public void onNotFound() {
                busy = false;
                toast(getString(R.string.sync_session_gone));
            }

            @Override
            public void onFailure(Exception e) {
                busy = false;
                toast(getString(SyncFragment.errorMessage(e)));
            }
        });
    }

    private void toast(String message) {
        Context context = getContext();
        if (context != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
