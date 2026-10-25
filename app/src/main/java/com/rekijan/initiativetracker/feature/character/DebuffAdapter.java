package com.rekijan.initiativetracker.feature.character;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.TextBinding;

/**
 * Edits the (de)buffs of one character: name, duration in rounds and description, each with a
 * remove button. Works directly on the character's own list.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<DebuffAdapter.DebuffViewHolder> {

    private final CharacterModel character;

    public DebuffAdapter(CharacterModel character) {
        this.character = character;
    }

    static class DebuffViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final Button removeButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.debuff_name_editText);
            durationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            descriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeButton = itemView.findViewById(R.id.remove_debuff_button);
        }
    }

    @NonNull
    @Override
    public DebuffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debuff, parent, false);
        return new DebuffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DebuffViewHolder holder, int position) {
        DebuffModel debuff = character.getDebuffList().get(position);

        TextBinding.bindText(holder.nameEditText, debuff.getName(), debuff::setName);
        TextBinding.bindInt(holder.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION,
                debuff::setDuration);
        TextBinding.bindText(holder.descriptionEditText, debuff.getDescription(), debuff::setDescription);

        holder.removeButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            character.getDebuffList().remove(pos);
            notifyItemRemoved(pos);
        });
    }

    @Override
    public int getItemCount() {
        return character.getDebuffList().size();
    }
}
