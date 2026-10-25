package com.rekijan.initiativetracker.feature.character;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.TextBinding;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.function.Consumer;

/**
 * The details of one character: PC or not, initiative bonus, max hp, fast healing,
 * regeneration, the free-text stat lines, notes and the (de)buffs. In portrait it replaces the
 * list; in landscape it is shown next to it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";

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
        titleTextView.setText(getString(R.string.character_details_name_title, character.getCharacterName()));

        CompoundButton pcSwitch = view.findViewById(R.id.is_pc_switch);
        pcSwitch.setChecked(character.isPC());
        pcSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            character.setPC(isChecked);
            encounter.notifyChanged();
        });

        TextBinding.bindInt(view.findViewById(R.id.initiative_bonus_editText), character.getInitiativeBonus(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiativeBonus);
        TextBinding.bindInt(view.findViewById(R.id.max_hp_editText), character.getMaxHp(),
                0, CharacterModel.MAX_HP, character::setMaxHp);
        TextBinding.bindInt(view.findViewById(R.id.fast_healing_editText), character.getFastHealing(),
                0, CharacterModel.MAX_HEALING, character::setFastHealing);
        TextBinding.bindInt(view.findViewById(R.id.regeneration_editText), character.getRegeneration(),
                0, CharacterModel.MAX_HEALING, character::setRegeneration);
        bindText(view, R.id.skills_editText, character.getSkills(), character::setSkills);
        bindText(view, R.id.attack_routine_editText, character.getAttackRoutine(), character::setAttackRoutine);
        bindText(view, R.id.ac_editText, character.getAc(), character::setAc);
        bindText(view, R.id.maneuvers_editText, character.getManeuvers(), character::setManeuvers);
        bindText(view, R.id.saves_editText, character.getSaves(), character::setSaves);
        bindText(view, R.id.notes_editText, character.getCharacterNotes(), character::setCharacterNotes);

        RecyclerView debuffRecyclerView = view.findViewById(R.id.debuff_recyclerView);
        debuffRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DebuffAdapter adapter = new DebuffAdapter(character);
        debuffRecyclerView.setAdapter(adapter);

        view.findViewById(R.id.clear_all_debuffs_button).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                character.getDebuffList().clear();
                adapter.notifyDataSetChanged();
                encounter.notifyChanged();
            }
        });
        view.findViewById(R.id.add_debuff_button).setOnClickListener(v -> {
            character.getDebuffList().add(new DebuffModel());
            adapter.notifyItemInserted(character.getDebuffList().size() - 1);
            encounter.notifyChanged();
        });
    }

    private static void bindText(View root, int id, String value, Consumer<String> onChanged) {
        EditText editText = root.findViewById(id);
        TextBinding.bindText(editText, value, onChanged);
    }
}
