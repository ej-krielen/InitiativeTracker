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

/**
 * The main screen: the round counter and the initiative order. Long press a card to remove that
 * character. The menu actions (next turn, sort, ...) are handled by {@link MainActivity}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class EncounterFragment extends Fragment implements Encounter.Listener {

    private Encounter encounter;
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
        encounter = ((InitiativeTrackerApp) requireActivity().getApplication()).getEncounter();

        roundTextView = view.findViewById(R.id.round_counter_textView);
        view.findViewById(R.id.plus_round_button).setOnClickListener(v -> encounter.nextRound());
        view.findViewById(R.id.minus_round_button).setOnClickListener(v -> encounter.previousRound());
        view.findViewById(R.id.reset_round_button).setOnClickListener(v -> encounter.resetRound());
        view.findViewById(R.id.edit_order_button).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showEditOrder());

        RecyclerView recyclerView = view.findViewById(R.id.characters_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CharacterAdapter(encounter, position ->
                ((MainActivity) requireActivity()).showCharacterDetail(position));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                this::askRemoveCharacter));

        encounter.addListener(this);
        onEncounterChanged();
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // Turns rotate the whole list
    @Override
    public void onEncounterChanged() {
        roundTextView.setText(String.valueOf(encounter.getRound()));
        adapter.notifyDataSetChanged();
    }

    private void askRemoveCharacter(int position) {
        if (!encounter.isValidPosition(position)) return;
        String name = encounter.get(position).getCharacterName();
        if (TextUtils.isEmpty(name)) name = getString(R.string.empty_character_name);
        Dialogs.builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete) + name + "?")
                .setPositiveButton(R.string.dialog_ok, (d, w) -> encounter.remove(position))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
