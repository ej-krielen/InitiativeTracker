package com.rekijan.initiativetracker.character.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.character.model.DebuffModel;
import com.rekijan.initiativetracker.listeners.GenericTextWatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the DebuffModel class
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<DebuffAdapter.DebuffViewHolder> {

    // Field for the list of CharacterModels
    private ArrayList<DebuffModel> debuffs = new ArrayList<>();
    // Reference to character to which the debuffs belong
    private CharacterModel character;

    // C'tor
    public DebuffAdapter(CharacterModel character) {
        this.character = character;
    }

    public void add(DebuffModel debuffs) {
        this.debuffs.add(debuffs);
    }

    public void addAll(List<DebuffModel> debuffs) {
        this.debuffs.addAll(debuffs);
    }

    /**
     * Remove a debuff based on position
     * @param position
     */
    public void remove(int position) {
        if (debuffs.size() > position) {
            debuffs.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove based on the debuff given
     * @param debuff
     */
    public void remove(DebuffModel debuff) {
        for (int i = 0; i < debuffs.size(); i++) {
            if (debuffs.get(i) == debuff) {
                remove(i);
            }
        }
    }

    public void removeAll() {
        debuffs.clear();
    }

    public ArrayList<DebuffModel> getList() {
        return debuffs;
    }

    /* ViewHolder region */
    public static class DebuffViewHolder extends RecyclerView.ViewHolder {
        CardView debuffCardView;
        EditText debuffNameEditText;
        EditText debuffDurationEditText;
        EditText debuffDescriptionEditText;
        Button removeDebuffButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            debuffCardView = itemView.findViewById(R.id.debuff_cardView);
            debuffNameEditText = itemView.findViewById(R.id.debuff_name_editText);
            debuffDurationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            debuffDescriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeDebuffButton = itemView.findViewById(R.id.remove_debuff_button);
        }
    }
    /* End of Viewholder region */

    @Override
    public DebuffViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.debuff_card, parent, false);
        return new DebuffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final DebuffViewHolder holder, int position) {
        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldDebuffNameWatcher = (GenericTextWatcher) holder.debuffNameEditText.getTag();
        if (oldDebuffNameWatcher != null) {
            holder.debuffNameEditText.removeTextChangedListener(oldDebuffNameWatcher);
        }
        GenericTextWatcher oldDebuffDurationWatcher = (GenericTextWatcher) holder.debuffDurationEditText.getTag();
        if (oldDebuffDurationWatcher != null) {
            holder.debuffDurationEditText.removeTextChangedListener(oldDebuffDurationWatcher);
        }
        GenericTextWatcher oldDebuffDescriptionWatcher = (GenericTextWatcher) holder.debuffDescriptionEditText.getTag();
        if (oldDebuffDescriptionWatcher != null) {
            holder.debuffDescriptionEditText.removeTextChangedListener(oldDebuffDescriptionWatcher);
        }

        //Get corresponding debuff
        final DebuffModel debuff = debuffs.get(position);

        //Set TextView and EditText values
        holder.debuffNameEditText.setText(debuff.getName());
        holder.debuffDurationEditText.setText(String.valueOf(debuff.getDuration()));
        holder.debuffDescriptionEditText.setText(debuff.getDescription());

        //Add new text watchers
        GenericTextWatcher newDebuffNameWatcher = new GenericTextWatcher(debuff, holder.debuffNameEditText);
        holder.debuffNameEditText.setTag(newDebuffNameWatcher);
        holder.debuffNameEditText.addTextChangedListener(newDebuffNameWatcher);

        GenericTextWatcher newDebuffDurationWatcher = new GenericTextWatcher(debuff, holder.debuffDurationEditText);
        holder.debuffDurationEditText.setTag(newDebuffDurationWatcher);
        holder.debuffDurationEditText.addTextChangedListener(newDebuffDurationWatcher);

        GenericTextWatcher newDebuffDescriptionWatcher = new GenericTextWatcher(debuff, holder.debuffDescriptionEditText);
        holder.debuffDescriptionEditText.setTag(newDebuffDescriptionWatcher);
        holder.debuffDescriptionEditText.addTextChangedListener(newDebuffDescriptionWatcher);

        holder.removeDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Remove from adapter and CharacterModel
                character.getDebuffList().remove(holder.getAdapterPosition());
                remove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return debuffs.size();
    }
}