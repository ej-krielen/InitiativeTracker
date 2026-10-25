package com.rekijan.initiativetracker.feature.sync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.party.Party;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * The Table sync screen, opened from the drawer. The Host tab lets the GM start a session for the
 * active party, shows the code to read out or share, the players who sent a character, and Stop
 * session. The Join tab lets a player join with a code and send their character form; the form
 * is saved on the phone. All the work is done by {@link SyncManager}; this screen only shows its
 * state and checks the form.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncFragment extends Fragment implements SyncManager.StateListener {

    private static final String STATE_TAB = "sync_tab";
    private static final int TAB_HOST = 0;
    private static final int TAB_JOIN = 1;

    private InitiativeTrackerApp app;
    private SyncManager sync;
    private TabLayout tabs;
    private View hostSection;
    private View joinSection;
    private boolean busy;

    public static SyncFragment newInstance() {
        return new SyncFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (InitiativeTrackerApp) requireActivity().getApplication();
        sync = app.getSync();
        hostSection = view.findViewById(R.id.sync_host_section);
        joinSection = view.findViewById(R.id.sync_join_section);
        tabs = view.findViewById(R.id.sync_tabs);

        // A player who joined comes back to the Join tab
        int tab = savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB, TAB_HOST)
                : sync.getHostCode() == null && sync.getPlayerCode() != null ? TAB_JOIN : TAB_HOST;
        TabLayout.Tab selected = tabs.getTabAt(tab);
        if (selected != null) selected.select();
        showTab(tab);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                showTab(t.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
                // Nothing to do
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
                // Nothing to do
            }
        });

        view.findViewById(R.id.sync_start_button).setOnClickListener(v -> startHosting());
        view.findViewById(R.id.sync_stop_button).setOnClickListener(v -> confirmStop());
        view.findViewById(R.id.sync_copy_button).setOnClickListener(v -> copyCode());
        view.findViewById(R.id.sync_share_button).setOnClickListener(v -> shareCode());
        view.findViewById(R.id.sync_join_button).setOnClickListener(v -> join());
        view.findViewById(R.id.sync_leave_button).setOnClickListener(v -> sync.leave());
        view.findViewById(R.id.sync_send_button).setOnClickListener(v -> send());
        EditText codeText = view.findViewById(R.id.sync_code_editText);
        codeText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            join();
            return true;
        });

        fillForm(sync.loadPlayerSheet());
        sync.addStateListener(this);
        render();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabs != null) outState.putInt(STATE_TAB, tabs.getSelectedTabPosition());
    }

    @Override
    public void onPause() {
        // Keep what the player typed, also numbers that are out of range (as their nearest value)
        sync.savePlayerSheet(readFormLenient());
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        sync.removeStateListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSyncStateChanged() {
        if (getView() != null) render();
    }

    // ---------------------------------------------------------------- Rendering

    private void showTab(int tab) {
        hostSection.setVisibility(tab == TAB_HOST ? View.VISIBLE : View.GONE);
        joinSection.setVisibility(tab == TAB_JOIN ? View.VISIBLE : View.GONE);
    }

    private void render() {
        View view = requireView();
        renderHost(view);
        renderJoin(view);
    }

    private void renderHost(View view) {
        String code = sync.getHostCode();
        boolean hosting = code != null;
        TextView intro = view.findViewById(R.id.sync_host_intro_textView);
        intro.setVisibility(hosting ? View.GONE : View.VISIBLE);
        intro.setText(getString(R.string.sync_host_intro, app.getParties().getActive().getName()));
        view.findViewById(R.id.sync_start_button).setVisibility(hosting ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.sync_code_card).setVisibility(hosting ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sync_players_label_textView).setVisibility(hosting ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sync_stop_button).setVisibility(hosting ? View.VISIBLE : View.GONE);
        LinearLayout list = view.findViewById(R.id.sync_players_list);
        list.removeAllViews();
        if (!hosting) return;

        ((TextView) view.findViewById(R.id.sync_code_textView)).setText(code);
        Party party = app.getParties().get(sync.getHostPartyId());
        String partyName = party != null ? party.getName() : "";
        String partyText = getString(R.string.sync_host_party, partyName);
        if (party != null && !party.getId().equals(app.getParties().getActiveId())) {
            partyText += "\n" + getString(R.string.sync_host_other_party, partyName);
        }
        ((TextView) view.findViewById(R.id.sync_host_party_textView)).setText(partyText);

        Map<String, PlayerSheet> players = sync.getPlayers();
        TextView label = view.findViewById(R.id.sync_players_label_textView);
        label.setText(players.isEmpty() ? getString(R.string.sync_players_none)
                : getString(R.string.sync_players_count, players.size()));
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (PlayerSheet sheet : players.values()) {
            View row = inflater.inflate(R.layout.item_sync_player, list, false);
            ((TextView) row.findViewById(R.id.sync_player_name_textView)).setText(sheet.name);
            ((TextView) row.findViewById(R.id.sync_player_stats_textView)).setText(
                    getString(R.string.sync_player_stats, sheet.initiative, sheet.hp, sheet.maxHp));
            list.addView(row);
        }
    }

    private void renderJoin(View view) {
        String code = sync.getPlayerCode();
        boolean joined = code != null;
        view.findViewById(R.id.sync_join_intro_textView).setVisibility(joined ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.sync_join_row).setVisibility(joined ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.sync_joined_card).setVisibility(joined ? View.VISIBLE : View.GONE);
        if (!joined) return;
        long sentAt = sync.getPlayerSentAt();
        String text = getString(R.string.sync_joined, code);
        if (sentAt > 0) {
            text += "\n" + getString(R.string.sync_last_sent,
                    DateFormat.getTimeFormat(requireContext()).format(new Date(sentAt)));
        }
        ((TextView) view.findViewById(R.id.sync_joined_textView)).setText(text);
    }

    // ---------------------------------------------------------------- Host actions

    private void startHosting() {
        if (busy) return;
        setBusy(true);
        sync.startHosting(new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                setBusy(false);
            }

            @Override
            public void onNotFound() {
                setBusy(false);
            }

            @Override
            public void onFailure(Exception e) {
                setBusy(false);
                toast(errorMessage(e));
            }
        });
    }

    private void confirmStop() {
        Dialogs.builder(requireContext())
                .setTitle(R.string.sync_stop_title)
                .setMessage(R.string.sync_stop_message)
                .setPositiveButton(R.string.sync_stop, (d, w) -> sync.stopHosting(
                        new SyncRepository.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                // The screen already shows Start session again
                            }

                            @Override
                            public void onFailure(Exception e) {
                                toast(R.string.sync_stop_offline);
                            }
                        }))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void copyCode() {
        String code = sync.getHostCode();
        if (code == null) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.sync_code_label), code));
        toast(R.string.sync_copied);
    }

    private void shareCode() {
        String code = sync.getHostCode();
        if (code == null) return;
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.sync_share_text, code));
        startActivity(Intent.createChooser(send, getString(R.string.sync_share)));
    }

    // ---------------------------------------------------------------- Player actions

    private void join() {
        if (busy) return;
        View view = requireView();
        TextInputLayout codeLayout = view.findViewById(R.id.sync_code_layout);
        EditText codeText = view.findViewById(R.id.sync_code_editText);
        String code = SessionCodes.normalize(codeText.getText().toString());
        if (!SessionCodes.isValid(code)) {
            codeLayout.setError(getString(R.string.sync_code_invalid, SessionCodes.LENGTH));
            return;
        }
        codeLayout.setError(null);
        setBusy(true);
        sync.join(code, new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                setBusy(false);
                codeText.setText("");
            }

            @Override
            public void onNotFound() {
                setBusy(false);
                if (getView() != null) codeLayout.setError(getString(R.string.sync_code_not_found));
            }

            @Override
            public void onFailure(Exception e) {
                setBusy(false);
                toast(errorMessage(e));
            }
        });
    }

    private void send() {
        if (busy) return;
        if (sync.getPlayerCode() == null) {
            toast(R.string.sync_join_first);
            return;
        }
        PlayerSheet sheet = readFormChecked();
        if (sheet == null) return;
        setBusy(true);
        sync.send(sheet, new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                setBusy(false);
                toast(R.string.sync_sent);
            }

            @Override
            public void onNotFound() {
                setBusy(false);
                toast(R.string.sync_session_gone);
            }

            @Override
            public void onFailure(Exception e) {
                setBusy(false);
                toast(errorMessage(e));
            }
        });
    }

    // ---------------------------------------------------------------- Form

    private void fillForm(PlayerSheet sheet) {
        View v = requireView();
        setText(v, R.id.sync_name_editText, sheet.name);
        setText(v, R.id.sync_initiative_editText, String.valueOf(sheet.initiative));
        setText(v, R.id.sync_initiative_bonus_editText, String.valueOf(sheet.initiativeBonus));
        setText(v, R.id.sync_hp_editText, String.valueOf(sheet.hp));
        setText(v, R.id.sync_max_hp_editText, String.valueOf(sheet.maxHp));
        setText(v, R.id.sync_ac_editText, sheet.ac);
        setText(v, R.id.sync_saves_editText, sheet.saves);
        setText(v, R.id.sync_maneuvers_editText, sheet.maneuvers);
        setText(v, R.id.sync_skills_editText, sheet.skills);
        setText(v, R.id.sync_attack_routine_editText, sheet.attackRoutine);
        setText(v, R.id.sync_notes_editText, sheet.notes);
    }

    /**
     * Reads the form and shows an error on every field that is wrong.
     *
     * @return The sheet, or null when something needs fixing first.
     */
    @Nullable
    private PlayerSheet readFormChecked() {
        View v = requireView();
        PlayerSheet sheet = readTexts(v);
        boolean ok = true;
        TextInputLayout nameLayout = v.findViewById(R.id.sync_name_layout);
        if (!sheet.hasName()) {
            nameLayout.setError(getString(R.string.sync_name_missing));
            ok = false;
        } else {
            nameLayout.setError(null);
        }
        Integer initiative = checkNumber(v, R.id.sync_initiative_layout, R.id.sync_initiative_editText,
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        Integer bonus = checkNumber(v, R.id.sync_initiative_bonus_layout, R.id.sync_initiative_bonus_editText,
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        Integer hp = checkNumber(v, R.id.sync_hp_layout, R.id.sync_hp_editText,
                CharacterModel.MIN_HP, CharacterModel.MAX_HP);
        Integer maxHp = checkNumber(v, R.id.sync_max_hp_layout, R.id.sync_max_hp_editText,
                0, CharacterModel.MAX_HP);
        if (initiative == null || bonus == null || hp == null || maxHp == null || !ok) return null;
        sheet.initiative = initiative;
        sheet.initiativeBonus = bonus;
        sheet.hp = hp;
        sheet.maxHp = maxHp;
        return sheet;
    }

    /** Reads the form without errors; numbers that don't read become their nearest valid value. */
    private PlayerSheet readFormLenient() {
        View v = requireView();
        PlayerSheet sheet = readTexts(v);
        sheet.initiative = lenient(v, R.id.sync_initiative_editText, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        sheet.initiativeBonus = lenient(v, R.id.sync_initiative_bonus_editText, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        sheet.hp = lenient(v, R.id.sync_hp_editText, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
        sheet.maxHp = lenient(v, R.id.sync_max_hp_editText, 0, CharacterModel.MAX_HP);
        return sheet;
    }

    private PlayerSheet readTexts(View v) {
        PlayerSheet sheet = new PlayerSheet();
        sheet.name = text(v, R.id.sync_name_editText);
        sheet.ac = text(v, R.id.sync_ac_editText);
        sheet.saves = text(v, R.id.sync_saves_editText);
        sheet.maneuvers = text(v, R.id.sync_maneuvers_editText);
        sheet.skills = text(v, R.id.sync_skills_editText);
        sheet.attackRoutine = text(v, R.id.sync_attack_routine_editText);
        sheet.notes = text(v, R.id.sync_notes_editText);
        return sheet;
    }

    private Integer checkNumber(View v, int layoutId, int editId, int min, int max) {
        TextInputLayout layout = v.findViewById(layoutId);
        Integer value = SyncForm.readNumber(text(v, editId), min, max);
        layout.setError(value == null ? getString(R.string.sync_number_range, min, max) : null);
        return value;
    }

    private int lenient(View v, int editId, int min, int max) {
        return NumberInput.parseInt(text(v, editId), 0, min, max);
    }

    private static String text(View v, int id) {
        EditText edit = v.findViewById(id);
        return edit.getText() != null ? edit.getText().toString() : "";
    }

    private static void setText(View v, int id, String value) {
        ((EditText) v.findViewById(id)).setText(value);
    }

    // ---------------------------------------------------------------- Helpers

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        View view = getView();
        if (view == null) return;
        int[] ids = {R.id.sync_start_button, R.id.sync_join_button, R.id.sync_send_button};
        for (int id : ids) {
            Button b = view.findViewById(id);
            b.setEnabled(!isBusy);
        }
    }

    @StringRes
    private static int errorMessage(Exception e) {
        if (e instanceof TimeoutException) return R.string.sync_error_offline;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return R.string.sync_error_offline;
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return R.string.sync_error_denied;
        }
        return R.string.sync_error_generic;
    }

    private void toast(@StringRes int message) {
        Context context = getContext();
        if (context != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
