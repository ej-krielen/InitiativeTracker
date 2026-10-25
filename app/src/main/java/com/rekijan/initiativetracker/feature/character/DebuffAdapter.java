package com.rekijan.initiativetracker.feature.character;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.TextBinding;

/**
 * Edits the (de)buffs of one character: name, duration in rounds and description, each with a
 * remove button. Works directly on the character's own list and reports every change, so the
 * sheet can update its count and the screen can save.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<DebuffAdapter.DebuffViewHolder> {

    private final CharacterModel character;
    private final Runnable onChanged;

    public DebuffAdapter(CharacterModel character, Runnable onChanged) {
        this.character = character;
        this.onChanged = onChanged;
    }

    static class DebuffViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final View removeButton;

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

        TextBinding.bindText(holder.nameEditText, debuff.getName(), text -> {
            debuff.setName(text);
            onChanged.run();
        });
        TextBinding.bindInt(holder.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION, value -> {
            debuff.setDuration(value);
            onChanged.run();
        });
        TextBinding.bindText(holder.descriptionEditText, debuff.getDescription(), text -> {
            debuff.setDescription(text);
            onChanged.run();
        });

        holder.removeButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            character.getDebuffList().remove(pos);
            notifyItemRemoved(pos);
            onChanged.run();
        });
    }

    @Override
    public int getItemCount() {
        return character.getDebuffList().size();
    }
}
