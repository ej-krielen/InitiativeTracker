package com.rekijan.initiativetracker.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.rekijan.initiativetracker.AppExtension;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.adapter.DebuffAdapter;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.character.model.DebuffModel;
import com.rekijan.initiativetracker.listeners.GenericTextWatcher;
import com.rekijan.initiativetracker.listeners.HpTextWatcher;

import java.util.ArrayList;

import static com.rekijan.initiativetracker.AppConstants.POSITION;

/**
 * A fragment containing details of a single CharacterModel
 */
public class CharacterDetailFragment extends Fragment {

    private int position;

    public CharacterDetailFragment() {}

    public static CharacterDetailFragment newInstance(int position) {
        CharacterDetailFragment fragment =  new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_character_detail, container, false);
        AppExtension app = (AppExtension) getActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getCharacterAdapter().getList().get(position);

        //Get fields and set their values
        TextView textView = rootView.findViewById(R.id.character_detail_textView);
        textView.setText(String.format(getString(R.string.character_details_name_title), characterModel.getCharacterName()));

        Switch pcSwitch = rootView.findViewById(R.id.is_pc_switch);
        pcSwitch.setChecked(characterModel.isPC());

        EditText initiativeBonusEditText = rootView.findViewById(R.id.initiative_bonus_editText);
        initiativeBonusEditText.setText(String.valueOf(characterModel.getInitiativeBonus()));

        EditText maxHpEditText = rootView.findViewById(R.id.max_hp_editText);
        maxHpEditText.setText(String.valueOf(characterModel.getMaxHp()));

        EditText skillsEditText = rootView.findViewById(R.id.skills_editText);
        skillsEditText.setText(characterModel.getSkills());

        EditText attackRoutineEditText = rootView.findViewById(R.id.attack_routine_editText);
        attackRoutineEditText.setText(characterModel.getAttackRoutine());

        EditText acEditText = rootView.findViewById(R.id.ac_editText);
        acEditText.setText(characterModel.getAc());

        EditText maneuversEditText = rootView.findViewById(R.id.maneuvers_editText);
        maneuversEditText.setText(characterModel.getManeuvers());

        EditText savesEditText = rootView.findViewById(R.id.saves_editText);
        savesEditText.setText(characterModel.getSaves());

        EditText notesEditText = rootView.findViewById(R.id.notes_editText);
        notesEditText.setText(characterModel.getCharacterNotes());

        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldInitiativeBonusWatcher = (GenericTextWatcher) initiativeBonusEditText.getTag();
        if (oldInitiativeBonusWatcher != null) {
            initiativeBonusEditText.removeTextChangedListener(oldInitiativeBonusWatcher);
        }

        GenericTextWatcher oldSkillsWatcher = (GenericTextWatcher) skillsEditText.getTag();
        if (oldSkillsWatcher != null) {
            skillsEditText.removeTextChangedListener(oldSkillsWatcher);
        }

        GenericTextWatcher oldAttackRoutineWatcher = (GenericTextWatcher) attackRoutineEditText.getTag();
        if (oldAttackRoutineWatcher != null) {
            attackRoutineEditText.removeTextChangedListener(oldAttackRoutineWatcher);
        }

        GenericTextWatcher oldAcWatcher = (GenericTextWatcher) acEditText.getTag();
        if (oldAcWatcher != null) {
            acEditText.removeTextChangedListener(oldAcWatcher);
        }

        GenericTextWatcher oldManeuversWatcher = (GenericTextWatcher) maneuversEditText.getTag();
        if (oldManeuversWatcher != null) {
            maneuversEditText.removeTextChangedListener(oldManeuversWatcher);
        }

        GenericTextWatcher oldSavesWatcher = (GenericTextWatcher) savesEditText.getTag();
        if (oldSavesWatcher != null) {
            savesEditText.removeTextChangedListener(oldSavesWatcher);
        }

        GenericTextWatcher oldNoteWatcher = (GenericTextWatcher) notesEditText.getTag();
        if (oldNoteWatcher != null) {
            notesEditText.removeTextChangedListener(oldNoteWatcher);
        }

        HpTextWatcher oldHPWatcher = (HpTextWatcher) maxHpEditText.getTag();
        if (oldHPWatcher != null) {
            maxHpEditText.setOnClickListener(null);
        }

        //Add new text watchers
        GenericTextWatcher newInitiativeBonusWatcher = new GenericTextWatcher(characterModel, initiativeBonusEditText);
        initiativeBonusEditText.setTag(newInitiativeBonusWatcher);
        initiativeBonusEditText.addTextChangedListener(newInitiativeBonusWatcher);

        GenericTextWatcher newSkillsWatcher = new GenericTextWatcher(characterModel, skillsEditText);
        skillsEditText.setTag(newSkillsWatcher);
        skillsEditText.addTextChangedListener(newSkillsWatcher);

        GenericTextWatcher newAttackRoutineWatcher = new GenericTextWatcher(characterModel, attackRoutineEditText);
        attackRoutineEditText.setTag(newAttackRoutineWatcher);
        attackRoutineEditText.addTextChangedListener(newAttackRoutineWatcher);

        GenericTextWatcher newAcWatcher = new GenericTextWatcher(characterModel, acEditText);
        acEditText.setTag(newAcWatcher);
        acEditText.addTextChangedListener(newAcWatcher);

        GenericTextWatcher newManeuversWatcher = new GenericTextWatcher(characterModel, maneuversEditText);
        maneuversEditText.setTag(newManeuversWatcher);
        maneuversEditText.addTextChangedListener(newManeuversWatcher);

        GenericTextWatcher newSavesWatcher = new GenericTextWatcher(characterModel, savesEditText);
        savesEditText.setTag(newSavesWatcher);
        savesEditText.addTextChangedListener(newSavesWatcher);

        GenericTextWatcher newNoteWatcher = new GenericTextWatcher(characterModel, notesEditText);
        notesEditText.setTag(newNoteWatcher);
        notesEditText.addTextChangedListener(newNoteWatcher);

        GenericTextWatcher newMaxHPWatcher = new GenericTextWatcher(characterModel, maxHpEditText);
        maxHpEditText.setTag(newMaxHPWatcher);
        maxHpEditText.addTextChangedListener(newMaxHPWatcher);

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView debuffRecyclerView = rootView.findViewById(R.id.debuff_recyclerView);
        debuffRecyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        debuffRecyclerView.setLayoutManager(llm);

        final DebuffAdapter adapter = new DebuffAdapter(characterModel);
        ArrayList<DebuffModel> debuffList = characterModel.getDebuffList();
        adapter.addAll(debuffList);
        debuffRecyclerView.setAdapter(adapter);

        //Buttons
        Button clearAllDebuffButton = rootView.findViewById(R.id.clear_all_debuffs_button);
        clearAllDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.removeAll();
                characterModel.getDebuffList().clear();
                adapter.notifyDataSetChanged();
            }
        });

        Button addDebuffButton = rootView.findViewById(R.id.add_debuff_button);
        addDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DebuffModel newDebuff = new DebuffModel();
                adapter.add(newDebuff);
                characterModel.getDebuffList().add(newDebuff);
                adapter.notifyDataSetChanged();
            }
        });

        pcSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                characterModel.setIsPC(isChecked);
            }
        });

        return rootView;
    }
}