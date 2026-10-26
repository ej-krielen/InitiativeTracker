package com.rekijan.initiativetracker.feature.encounter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.app.MainActivity;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.core.util.RecyclerLongPressListener;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.sync.SyncManager;
import com.rekijan.initiativetracker.feature.sync.SyncRules;

import java.util.Collections;

/**
 * The main screen: the initiative order with the turn bar under it (the round and Next turn).
 * Long press a card to remove that character; a synced PC can also be unlinked there, and its
 * player is told. Next turn, Sort and PCs to max HP (toolbar) and the
 * drawer actions are handled by {@link MainActivity}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class EncounterFragment extends Fragment implements Encounter.Listener, SyncManager.StateListener {

    private Encounter encounter;
    private SyncManager sync;
    private CharacterAdapter adapter;
    private TextView roundTextView;

    public static EncounterFragment newInstance() {
        return new EncounterFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_encounter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InitiativeTrackerApp app = (InitiativeTrackerApp) requireActivity().getApplication();
        encounter = app.getEncounter();
        sync = app.getSync();

        roundTextView = view.findViewById(R.id.round_counter_textView);
        view.findViewById(R.id.plus_round_button).setOnClickListener(v -> encounter.nextRound());
        view.findViewById(R.id.minus_round_button).setOnClickListener(v -> encounter.previousRound());
        View turnBar = view.findViewById(R.id.turn_bar);
        view.findViewById(R.id.next_turn_button).setOnClickListener(v ->
                ((MainActivity) requireActivity()).nextTurn(turnBar));

        RecyclerView recyclerView = view.findViewById(R.id.characters_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CharacterAdapter(encounter, position ->
                ((MainActivity) requireActivity()).showCharacterDetail(position), sync::getStatus);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                this::askRemoveCharacter));

        encounter.addListener(this);
        sync.addStateListener(this);
        onEncounterChanged();
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        sync.removeStateListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // Turns rotate the whole list
    @Override
    public void onEncounterChanged() {
        roundTextView.setText(String.valueOf(encounter.getRound()));
        adapter.notifyDataSetChanged();
    }

    /** Players' sheets came or went: the Synced and Player left tags change. */
    @SuppressLint("NotifyDataSetChanged") // Any card's tag may change
    @Override
    public void onSyncStateChanged() {
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void askRemoveCharacter(int position) {
        if (!encounter.isValidPosition(position)) return;
        CharacterModel character = encounter.get(position);
        String name = character.getCharacterName();
        if (TextUtils.isEmpty(name)) name = getString(R.string.empty_character_name);
        if (sync.getStatus(character) != SyncRules.Status.NONE) {
            Dialogs.builder(requireContext())
                    .setTitle(getString(R.string.sync_remove_title, name))
                    .setMessage(getString(R.string.sync_remove_message, name))
                    .setPositiveButton(R.string.sync_remove, (d, w) ->
                            sync.removeCharacters(Collections.singletonList(character)))
                    .setNeutralButton(R.string.sync_keep_unlink, (d, w) -> sync.unlink(character))
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return;
        }
        Dialogs.builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete) + name + "?")
                .setPositiveButton(R.string.dialog_ok, (d, w) ->
                        sync.removeCharacters(Collections.singletonList(character)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
