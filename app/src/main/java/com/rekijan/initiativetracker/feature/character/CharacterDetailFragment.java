package com.rekijan.initiativetracker.feature.character;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.spell.SpellDialogs;
import com.rekijan.initiativetracker.feature.sync.SyncManager;
import com.rekijan.initiativetracker.feature.sync.SyncRules;

/**
 * The details of one character in the GM's encounter: the name on top, then the sheet with the
 * Stats and Conditions tabs ({@link CharacterSheet}). In portrait it replaces the list; in
 * landscape it is shown next to it. The open tab survives rotation. While hosting Table sync, a
 * synced PC shows that under the name, with Unlink: the GM runs it from then on.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment implements SyncManager.StateListener {

    private static final String ARG_POSITION = "POSITION";
    private static final String STATE_TAB = "sheet_tab";

    private CharacterSheet sheet;
    private SyncManager sync;
    private CharacterModel character;

    public static CharacterDetailFragment newInstance(int position) {
        CharacterDetailFragment fragment = new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /** @return The position in the encounter of the character shown here. */
    public int getPosition() {
        return requireArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InitiativeTrackerApp app = (InitiativeTrackerApp) requireActivity().getApplication();
        Encounter encounter = app.getEncounter();
        sync = app.getSync();
        int position = getPosition();
        if (!encounter.isValidPosition(position)) {
            // The character was removed (e.g. before a rotation); show nothing
            view.setVisibility(View.INVISIBLE);
            return;
        }
        character = encounter.get(position);

        TextView titleTextView = view.findViewById(R.id.character_detail_textView);
        String name = character.getCharacterName();
        titleTextView.setText(TextUtils.isEmpty(name) ? getString(R.string.empty_character_name) : name);

        sheet = new CharacterSheet(view.findViewById(R.id.character_sheet), false, encounter::notifyChanged);
        sheet.setOnEndSpell(spell -> SpellDialogs.askEnd(requireContext(), encounter, spell.getSpellId(),
                spell.getName(), false, sheet::refreshConditions));
        sheet.bind(character);
        if (savedInstanceState != null) {
            sheet.selectTab(savedInstanceState.getInt(STATE_TAB, CharacterSheet.TAB_STATS));
        }

        String shownName = titleTextView.getText().toString();
        view.findViewById(R.id.character_unlink_button).setOnClickListener(v -> Dialogs.builder(requireContext())
                .setTitle(getString(R.string.sync_unlink_title, shownName))
                .setMessage(getString(R.string.sync_unlink_message, shownName))
                .setPositiveButton(R.string.sync_unlink, (d, w) -> sync.unlink(character))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show());
        renderSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (sync != null) sync.addStateListener(this);
        renderSync();
    }

    @Override
    public void onStop() {
        if (sync != null) sync.removeStateListener(this);
        super.onStop();
    }

    @Override
    public void onSyncStateChanged() {
        renderSync();
    }

    /** The sync row: shown while the character is synced, or linked to a player who left. */
    private void renderSync() {
        View view = getView();
        if (view == null || character == null) return;
        SyncRules.Status status = sync.getStatus(character);
        view.findViewById(R.id.character_sync_row).setVisibility(
                status == SyncRules.Status.NONE ? View.GONE : View.VISIBLE);
        ((TextView) view.findViewById(R.id.character_sync_status_textView)).setText(
                status == SyncRules.Status.SYNCED ? R.string.sync_linked_synced : R.string.sync_linked_player_left);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sheet != null) outState.putInt(STATE_TAB, sheet.getSelectedTab());
    }
}
