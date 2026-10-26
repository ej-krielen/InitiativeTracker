package com.rekijan.initiativetracker.feature.character;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.core.util.TextBinding;

import java.util.function.Consumer;

/**
 * Edits the conditions of one character: name, duration in rounds (empty for none) and
 * description, each with a remove button. Works directly on the character's own list and reports every change, so the
 * sheet can update its count and the screen can save. A spell shows who cast it, and for the GM
 * an End on all targets button.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<DebuffAdapter.DebuffViewHolder> {

    private final CharacterModel character;
    private final Runnable onChanged;
    @Nullable
    private final Consumer<DebuffModel> onEndSpell;

    /**
     * @param onEndSpell Ends a spell on all its targets (GM only), or null to leave the button out.
     */
    public DebuffAdapter(CharacterModel character, Runnable onChanged, @Nullable Consumer<DebuffModel> onEndSpell) {
        this.character = character;
        this.onChanged = onChanged;
        this.onEndSpell = onEndSpell;
    }

    static class DebuffViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final View removeButton;
        final View spellRow;
        final TextView casterTextView;
        final View endAllButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.debuff_name_editText);
            durationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            descriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeButton = itemView.findViewById(R.id.remove_debuff_button);
            spellRow = itemView.findViewById(R.id.debuff_spell_row);
            casterTextView = itemView.findViewById(R.id.debuff_caster_textView);
            endAllButton = itemView.findViewById(R.id.debuff_end_all_button);
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
        // No duration (0) shows as an empty field
        TextBinding.bindText(holder.durationEditText, debuff.getDuration() == 0 ? "" : String.valueOf(debuff.getDuration()), text -> {
            debuff.setDuration(NumberInput.parseInt(text, 0, 0, DebuffModel.MAX_DURATION));
            onChanged.run();
        });
        TextBinding.bindText(holder.descriptionEditText, debuff.getDescription(), text -> {
            debuff.setDescription(text);
            onChanged.run();
        });

        String caster = debuff.getCasterName();
        holder.spellRow.setVisibility(caster.isEmpty() ? View.GONE : View.VISIBLE);
        holder.casterTextView.setText(holder.itemView.getContext().getString(R.string.debuff_cast_by, caster));
        boolean canEndAll = onEndSpell != null && debuff.getSpellId() != null;
        holder.endAllButton.setVisibility(canEndAll ? View.VISIBLE : View.GONE);
        holder.endAllButton.setOnClickListener(v -> {
            if (onEndSpell != null) onEndSpell.accept(debuff);
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
