package com.rekijan.initiativetracker.character.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

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
     * Remove a character based on position, flag another with isFirstRound if the one being removed is currently flagged thus
     * @param position
     */
    public void remove(int position) {
        if (characters.size() > position)
            if (characters.get(position).isFirstRound()) {
                {
                    if (position-1 < 0) {
                        characters.get(characters.size()-1).setIsFirstRound(true);
                    } else {
                        characters.get(position-1).setIsFirstRound(true);
                    }
                }
                characters.remove(position);
            } else {
                characters.remove(position);
            }
        this.notifyDataSetChanged();
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
    public boolean sortInitiative() {
        Collections.sort(characters, new Comparator<CharacterModel>() {
            public int compare(CharacterModel o1, CharacterModel o2) {
                return o2.getInitiative() - o1.getInitiative();
            }
        });

        for (CharacterModel c : characters) {
            c.setIsFirstRound(false);
        }
        characters.get(0).setIsFirstRound(true);
        this.notifyDataSetChanged();

        return checkForDoubleInitiative();
    }

    private boolean checkForDoubleInitiative() {

        boolean doubleInitiativeDetected = false;
        for (CharacterModel m : characters) {
            for (CharacterModel m2 : characters) {
                if (m != m2 && m2.getInitiative() == m.getInitiative()) {
                    doubleInitiativeDetected = true;
                }
            }
        }
        return doubleInitiativeDetected;
    }

    /**
     * Each {@link CharacterModel} is pushed down the list by one, bottom one becomes the top
     * @return true if its the first character in the round
     * @param activity
     */
    public boolean nextTurn(FragmentActivity activity) {
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
        characters.get(0).updateDebuffs(activity);
        this.notifyDataSetChanged();
        return characters.get(0).isFirstRound();
    }

    public void removeAll() {
        characters.clear();
    }

    /**
     * Goes through all characters and checks if they are a PC.<br>
     *     If the pc's max hp value is higher then its current hp value it gets updated,<br>
     *         but if not its name is added to a list and displayed so the user know.
     *
     */
    public void setPCsToMaxHP() {
        StringBuilder unUpdatedPcNames = new StringBuilder();
        for (CharacterModel c : characters) {
            if (c.isPC()) {
                if (c.getMaxHp() > c.getHp()) {
                    c.setHp(c.getMaxHp());
                } else {
                    unUpdatedPcNames.append(c.getCharacterName());
                    unUpdatedPcNames.append("\n");
                }
            }
        }
        String toastMessage = context.getString(R.string.toast_max_hp, unUpdatedPcNames.toString());
        this.notifyDataSetChanged();
        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
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

        // Set background color depending on whether the character is a PC or NPC
        int color = character.isPC() ? context.getResources().getColor(R.color.pcColor) : context.getResources().getColor(R.color.npcColor);
        ((CardView) holder.itemView).setCardBackgroundColor(color);

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