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
import com.rekijan.initiativetracker.feature.encounter.Encounter;

/**
 * The details of one character in the GM's encounter: the name on top, then the sheet with the
 * Stats and Conditions tabs ({@link CharacterSheet}). In portrait it replaces the list; in
 * landscape it is shown next to it. The open tab survives rotation.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";
    private static final String STATE_TAB = "sheet_tab";

    private CharacterSheet sheet;

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
        Encounter encounter = ((InitiativeTrackerApp) requireActivity().getApplication()).getEncounter();
        int position = getPosition();
        if (!encounter.isValidPosition(position)) {
            // The character was removed (e.g. before a rotation); show nothing
            view.setVisibility(View.INVISIBLE);
            return;
        }
        CharacterModel character = encounter.get(position);

        TextView titleTextView = view.findViewById(R.id.character_detail_textView);
        String name = character.getCharacterName();
        titleTextView.setText(TextUtils.isEmpty(name) ? getString(R.string.empty_character_name) : name);

        sheet = new CharacterSheet(view.findViewById(R.id.character_sheet), false, encounter::notifyChanged);
        sheet.bind(character);
        if (savedInstanceState != null) {
            sheet.selectTab(savedInstanceState.getInt(STATE_TAB, CharacterSheet.TAB_STATS));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sheet != null) outState.putInt(STATE_TAB, sheet.getSelectedTab());
    }
}
