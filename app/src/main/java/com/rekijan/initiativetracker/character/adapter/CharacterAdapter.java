package com.rekijan.initiativetracker.character.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.character.model.DebuffModel;
import com.rekijan.initiativetracker.listeners.GenericTextWatcher;
import com.rekijan.initiativetracker.listeners.HpTextWatcher;
import com.rekijan.initiativetracker.ui.activities.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015 Creation of this file
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    // Field for the list of CharacterModels
    private ArrayList<CharacterModel> characters = new ArrayList<>();
    // Passing along the activity is needed to build and populate dialogs
    private Context context;

    // C'tor
    public CharacterAdapter(Context context) {
        this.context = context;
    }

    public void add(CharacterModel character) {
        characters.add(character);
    }

    public void addAll(List<CharacterModel> characters) {
        this.characters.addAll(characters);
    }

    /**
     * Remove a character based on position
     * @param position
     */
    public void remove(int position) {
        if (characters.size() > position) {
            characters.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove based on the character given
     * @param character
     */
    public void remove(CharacterModel character) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i) == character) {
                remove(i);
            }
        }
    }

    public ArrayList<CharacterModel> getList() {
        return characters;
    }

    /**
     * Sorts the {@link #characters} list by {@link CharacterModel#getInitiative()}. High to low. <br>
     *     And make the first character the first in the round
     */
    public void sortInitiative() {
        Collections.sort(characters, new Comparator<CharacterModel>() {
            public int compare(CharacterModel o1, CharacterModel o2) {
                return o2.getInitiative() - o1.getInitiative();
            }
        });

        for (CharacterModel c : characters) {
            c.setIsFirstRound(false);
        }
        characters.get(0).setIsFirstRound(true);
        notifyDataSetChanged();
    }

    /**
     * Each {@link CharacterModel} is pushed down the list by one, bottom one becomes the top
     * @return true if its the first character in the round
     */
    public boolean nextTurn() {
        //Create temporary list
        ArrayList<CharacterModel> newList = new ArrayList<>();
        //Add all the items in the new order
        for (int i = 1; i < characters.size(); i++) {
            newList.add(characters.get(i));
        }
        newList.add(characters.get(0));

        //Fill the old list with the new temporary one
        characters = newList;
        //Update the (de)buffs of the top character
        characters.get(0).updateDebuffs();
        notifyDataSetChanged();
        return characters.get(0).isFirstRound();
    }

    public void removeAll() {
        characters.clear();
    }


    /* ViewHolder region */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView characterCardView;
        EditText characterInitiativeEditText;
        EditText characterNameEditText;
        TextView characterHpEditText;
        TextView debuffOverviewTextView;
        Button showCharacterDetailButton;

        CharacterViewHolder(View itemView) {
            super(itemView);
            characterCardView = itemView.findViewById(R.id.character_cardView);
            characterInitiativeEditText = itemView.findViewById(R.id.character_initiative_editText);
            characterNameEditText = itemView.findViewById(R.id.character_name_editText);
            characterHpEditText = itemView.findViewById(R.id.debuff_duration_editText);
            showCharacterDetailButton = itemView.findViewById(R.id.show_character_detail_button);
            debuffOverviewTextView = itemView.findViewById(R.id.debuff_description_editText);
        }
    }
    /* End of Viewholder region */


    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.character_card, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CharacterViewHolder holder, int position) {
        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldInitiativeWatcher = (GenericTextWatcher) holder.characterInitiativeEditText.getTag();
        if (oldInitiativeWatcher != null) {
            holder.characterInitiativeEditText.removeTextChangedListener(oldInitiativeWatcher);
        }
        GenericTextWatcher oldNameWatcher = (GenericTextWatcher) holder.characterNameEditText.getTag();
        if (oldNameWatcher != null) {
            holder.characterNameEditText.removeTextChangedListener(oldNameWatcher);
        }
        HpTextWatcher oldHPWatcher = (HpTextWatcher) holder.characterHpEditText.getTag();
        if (oldHPWatcher != null) {
            holder.characterHpEditText.setOnClickListener(null);
        }

        // Get the corresponding CharacterModel
        final CharacterModel character = characters.get(position);

        // Set values TextViews and EditTexts
        holder.characterInitiativeEditText.setText(String.valueOf(character.getInitiative()));

        if (character.getCharacterName() != null) {
            holder.characterNameEditText.setText(character.getCharacterName());
        }

        holder.characterHpEditText.setText(String.valueOf(character.getHp()));

        //Add new text watchers
        GenericTextWatcher newInitiativeWatcher = new GenericTextWatcher(character, holder.characterInitiativeEditText);
        holder.characterInitiativeEditText.setTag(newInitiativeWatcher);
        holder.characterInitiativeEditText.addTextChangedListener(newInitiativeWatcher);

        GenericTextWatcher newNameWatcher = new GenericTextWatcher(character, holder.characterNameEditText);
        holder.characterNameEditText.setTag(newNameWatcher);
        holder.characterNameEditText.addTextChangedListener(newNameWatcher);

        HpTextWatcher newHPWatcher = new HpTextWatcher(character, this, context);
        holder.characterHpEditText.setTag(newHPWatcher);
        holder.characterHpEditText.setOnClickListener(newHPWatcher);

        ArrayList<DebuffModel> debuffs = character.getDebuffList();
        if (debuffs.size() > 0) {
            holder.debuffOverviewTextView.setVisibility(View.VISIBLE);
            StringBuilder debuffString = new StringBuilder();
            debuffString.append(context.getResources().getString(R.string.char_card_debuffs));
            for (DebuffModel d: debuffs) {
                debuffString.append(" ");
                debuffString.append(d.getName());
                debuffString.append(" (");
                debuffString.append(d.getDuration());
                debuffString.append("), ");
            }
            debuffString.delete(debuffString.length()-2, debuffString.length()); //Remove last comma and space
            holder.debuffOverviewTextView.setText(debuffString.toString());
        } else {
            holder.debuffOverviewTextView.setVisibility(View.GONE); //Don't show TextView if list is empty
        }

        holder.showCharacterDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).replaceCharacterDetailFragment(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}