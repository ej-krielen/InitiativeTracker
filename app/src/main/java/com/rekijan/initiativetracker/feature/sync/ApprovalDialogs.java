package com.rekijan.initiativetracker.feature.sync;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The GM's popups for Table sync requests. A new player: "Valeros wants to join" with their
 * stats, Add to party or Reject. A known player: a checklist with one row per change, all ticked;
 * the GM unticks what they don't accept and taps Apply, or Refuse all. Neither can be dismissed
 * without an answer, since the player waits for it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class ApprovalDialogs {

    /** What the GM decided. */
    public interface Listener {
        void onApprove(List<SyncDiff.Change> accepted);

        void onRefuse();
    }

    /** Values longer than this are shown as "changed" with the new text below. */
    private static final int SHORT_VALUE = 24;

    private ApprovalDialogs() {
    }

    /** @return The shown dialog, so the caller knows when it is gone. */
    public static AlertDialog showJoin(Context context, PlayerSheet sheet, String partyName, Listener listener) {
        String name = sheet.name;
        String stats = context.getString(R.string.approval_join_stats, sheet.initiative, sheet.hp, sheet.maxHp,
                sheet.ac.isEmpty() ? "–" : sheet.ac);
        String conditions = context.getResources().getQuantityString(
                R.plurals.approval_join_conditions, sheet.conditions.size(), sheet.conditions.size());
        return Dialogs.builder(context)
                .setTitle(R.string.approval_join_title)
                .setMessage(context.getString(R.string.approval_join_message, name, partyName) + "\n\n" + stats + "\n" + conditions)
                .setCancelable(false)
                .setNegativeButton(R.string.approval_reject, (d, w) -> listener.onRefuse())
                .setPositiveButton(R.string.approval_add, (d, w) -> listener.onApprove(new ArrayList<>()))
                .show();
    }

    /** @return The shown dialog, so the caller knows when it is gone. */
    public static AlertDialog showChanges(Context context, String name, List<SyncDiff.Change> changes, Listener listener) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_approval, null);
        LinearLayout list = content.findViewById(R.id.approval_list);
        List<CheckBox> boxes = new ArrayList<>();
        int muted = MaterialColors.getColor(content, com.google.android.material.R.attr.colorOnSurfaceVariant);
        for (SyncDiff.Change change : changes) {
            CheckBox box = (CheckBox) LayoutInflater.from(context).inflate(R.layout.item_approval_change, list, false);
            box.setText(text(context, change, muted));
            box.setChecked(true);
            list.addView(box);
            boxes.add(box);
        }

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.approval_changes_title,
                        changes.size(), name, changes.size()))
                .setView(content)
                .setCancelable(false)
                .setNegativeButton(R.string.approval_refuse_all, (d, w) -> listener.onRefuse())
                .setPositiveButton(R.string.approval_apply, null)
                .create();
        Runnable updateApply = () -> {
            int count = 0;
            for (CheckBox b : boxes) if (b.isChecked()) count++;
            Button apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (apply != null) apply.setText(context.getString(R.string.approval_apply_count, count));
        };
        for (CheckBox b : boxes) b.setOnCheckedChangeListener((v, checked) -> updateApply.run());
        dialog.setOnShowListener(d -> {
            updateApply.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                List<SyncDiff.Change> accepted = new ArrayList<>();
                for (int i = 0; i < boxes.size(); i++) {
                    if (boxes.get(i).isChecked()) accepted.add(changes.get(i));
                }
                // Answer first, then close: closing shows the next request
                if (accepted.isEmpty()) {
                    // Nothing ticked is the same as refusing all
                    listener.onRefuse();
                } else {
                    listener.onApprove(accepted);
                }
                dialog.dismiss();
            });
        });
        dialog.show();
        return dialog;
    }

    /** @return The change as a line, with a smaller muted line below for long values or a description. */
    static CharSequence text(Context context, SyncDiff.Change change, int mutedColor) {
        String title;
        String detail = null;
        switch (change.kind) {
            case CONDITION_ADDED:
                title = context.getString(R.string.approval_condition_added, name(context, change.condition),
                        change.condition.getDuration());
                detail = change.condition.getDescription();
                break;
            case CONDITION_REMOVED:
                title = context.getString(R.string.approval_condition_removed,
                        change.from.isEmpty() ? context.getString(R.string.approval_condition_unnamed) : change.from);
                break;
            case CONDITION_CHANGED:
                title = context.getString(R.string.approval_condition_changed, name(context, change.condition),
                        change.condition.getDuration());
                detail = change.condition.getDescription();
                break;
            default: {
                String field = context.getString(label(change.field));
                if (change.from.length() <= SHORT_VALUE && change.to.length() <= SHORT_VALUE) {
                    title = context.getString(R.string.approval_field_change, field,
                            change.from.isEmpty() ? "–" : change.from, change.to.isEmpty() ? "–" : change.to);
                } else {
                    title = context.getString(R.string.approval_field_changed, field);
                    detail = change.to;
                }
                break;
            }
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(title);
        if (detail != null && !detail.isEmpty()) {
            int start = sb.length();
            sb.append('\n').append(detail.length() > 120 ? detail.substring(0, 120) + "…" : detail);
            sb.setSpan(new ForegroundColorSpan(mutedColor), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.85f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private static String name(Context context, DebuffModel d) {
        return d.getName() == null || d.getName().trim().isEmpty()
                ? context.getString(R.string.approval_condition_unnamed) : d.getName();
    }

    @StringRes
    static int label(SyncDiff.Field field) {
        switch (field) {
            case NAME: return R.string.sync_field_name;
            case INITIATIVE: return R.string.sync_field_initiative;
            case INITIATIVE_BONUS: return R.string.char_detail_initiative_bonus;
            case HP: return R.string.sync_field_hp;
            case MAX_HP: return R.string.char_detail_max_hp;
            case FAST_HEALING: return R.string.char_detail_fast_healing;
            case REGENERATION: return R.string.char_detail_regeneration;
            case AC: return R.string.approval_field_ac;
            case SAVES: return R.string.approval_field_saves;
            case MANEUVERS: return R.string.char_detail_maneuvers;
            case SKILLS: return R.string.char_detail_skills;
            case ATTACK_ROUTINE: return R.string.char_detail_attack_routine;
            case NOTES: return R.string.char_detail_notes;
            default: return R.string.sheet_tab_conditions;
        }
    }
}
