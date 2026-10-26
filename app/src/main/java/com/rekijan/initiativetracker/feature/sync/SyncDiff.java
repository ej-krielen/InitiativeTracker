package com.rekijan.initiativetracker.feature.sync;

import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The differences between two {@link PlayerSheet}s, as a list of {@link Change}s: one per field
 * (HP 20 → 30) and one per condition that was added, removed or changed (matched by name). The
 * GM ticks the changes to accept in the approval popup and {@link #apply} takes only those; the
 * player's screen uses the same list to mark what is not sent yet. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SyncDiff {

    /** The fields of a sheet, in the order they are listed. */
    public enum Field {
        NAME, INITIATIVE, INITIATIVE_BONUS, HP, MAX_HP, FAST_HEALING, REGENERATION,
        AC, SAVES, MANEUVERS, SKILLS, ATTACK_ROUTINE, NOTES, CONDITIONS
    }

    /** What kind of change. */
    public enum Kind {
        /** A field other than the conditions has another value. */
        FIELD,
        CONDITION_ADDED,
        CONDITION_REMOVED,
        /** Same name, other rounds or description. */
        CONDITION_CHANGED
    }

    /** One change from one sheet to the other. */
    public static final class Change {
        public final Kind kind;
        public final Field field;
        /** For a field: the old and new value as text. For a condition: its name and its rounds. */
        public final String from;
        public final String to;
        /** For a condition change: the condition as it is after the change (null when removed). */
        public final DebuffModel condition;
        /** Unique within one list; identifies the change when the GM ticks it. */
        public final String key;

        Change(Kind kind, Field field, String from, String to, DebuffModel condition, String key) {
            this.kind = kind;
            this.field = field;
            this.from = from;
            this.to = to;
            this.condition = condition;
            this.key = key;
        }
    }

    private SyncDiff() {
    }

    /** @return What changes from {@code from} to {@code to}; empty when they are the same. */
    public static List<Change> changes(PlayerSheet from, PlayerSheet to) {
        PlayerSheet a = from.cleaned();
        PlayerSheet b = to.cleaned();
        List<Change> list = new ArrayList<>();
        field(list, Field.NAME, a.name, b.name);
        field(list, Field.INITIATIVE, a.initiative, b.initiative);
        field(list, Field.INITIATIVE_BONUS, a.initiativeBonus, b.initiativeBonus);
        field(list, Field.HP, a.hp, b.hp);
        field(list, Field.MAX_HP, a.maxHp, b.maxHp);
        field(list, Field.FAST_HEALING, a.fastHealing, b.fastHealing);
        field(list, Field.REGENERATION, a.regeneration, b.regeneration);
        field(list, Field.AC, a.ac, b.ac);
        field(list, Field.SAVES, a.saves, b.saves);
        field(list, Field.MANEUVERS, a.maneuvers, b.maneuvers);
        field(list, Field.SKILLS, a.skills, b.skills);
        field(list, Field.ATTACK_ROUTINE, a.attackRoutine, b.attackRoutine);
        field(list, Field.NOTES, a.notes, b.notes);
        conditions(list, a.conditions, b.conditions);
        return list;
    }

    /** @return The fields that differ; CONDITIONS when any condition differs. */
    public static List<Field> changedFields(PlayerSheet from, PlayerSheet to) {
        List<Field> fields = new ArrayList<>();
        for (Change c : changes(from, to)) {
            if (!fields.contains(c.field)) fields.add(c.field);
        }
        return fields;
    }

    /**
     * @param target   The sheet the changes are made on (the GM's character).
     * @param source   The sheet the changes come from (the player's send).
     * @param accepted The changes from {@code changes(target, source)} to take.
     * @return A new sheet: the target with only the accepted changes made.
     */
    public static PlayerSheet apply(PlayerSheet target, PlayerSheet source, Collection<Change> accepted) {
        PlayerSheet r = target.cleaned();
        PlayerSheet s = source.cleaned();
        for (Change c : accepted) {
            switch (c.kind) {
                case FIELD:
                    copyField(r, s, c.field);
                    break;
                case CONDITION_ADDED:
                    r.conditions.add(copy(c.condition));
                    break;
                case CONDITION_REMOVED: {
                    int i = indexOf(r.conditions, c.from);
                    if (i >= 0) r.conditions.remove(i);
                    break;
                }
                case CONDITION_CHANGED: {
                    int i = indexOf(r.conditions, c.from);
                    if (i >= 0) r.conditions.set(i, copy(c.condition));
                    break;
                }
                default:
                    break;
            }
        }
        return r.cleaned();
    }

    /** @return True when the two sheets share exactly the same conditions, in the same order. */
    static boolean sameConditions(List<DebuffModel> a, List<DebuffModel> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!same(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- Private

    private static void field(List<Change> list, Field field, Object a, Object b) {
        if (Objects.equals(a, b)) return;
        list.add(new Change(Kind.FIELD, field, String.valueOf(a), String.valueOf(b), null, field.name()));
    }

    /**
     * Pairs conditions by name (ignoring case), in order, so two conditions with the same name are
     * paired first with first. Unpaired ones in {@code b} are added, in {@code a} removed.
     */
    private static void conditions(List<Change> list, List<DebuffModel> a, List<DebuffModel> b) {
        boolean[] used = new boolean[a.size()];
        int n = 0;
        for (DebuffModel nb : b) {
            int match = -1;
            for (int i = 0; i < a.size(); i++) {
                if (!used[i] && key(a.get(i)).equals(key(nb))) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                list.add(new Change(Kind.CONDITION_ADDED, Field.CONDITIONS, nb.getName(),
                        String.valueOf(nb.getDuration()), copy(nb), "C+" + (n++) + ":" + key(nb)));
                continue;
            }
            used[match] = true;
            DebuffModel na = a.get(match);
            if (!same(na, nb)) {
                list.add(new Change(Kind.CONDITION_CHANGED, Field.CONDITIONS, na.getName(),
                        String.valueOf(nb.getDuration()), copy(nb), "C~" + (n++) + ":" + key(nb)));
            }
        }
        for (int i = 0; i < a.size(); i++) {
            if (!used[i]) {
                list.add(new Change(Kind.CONDITION_REMOVED, Field.CONDITIONS, a.get(i).getName(),
                        "", null, "C-" + (n++) + ":" + key(a.get(i))));
            }
        }
    }

    private static void copyField(PlayerSheet r, PlayerSheet s, Field field) {
        switch (field) {
            case NAME: r.name = s.name; break;
            case INITIATIVE: r.initiative = s.initiative; break;
            case INITIATIVE_BONUS: r.initiativeBonus = s.initiativeBonus; break;
            case HP: r.hp = s.hp; break;
            case MAX_HP: r.maxHp = s.maxHp; break;
            case FAST_HEALING: r.fastHealing = s.fastHealing; break;
            case REGENERATION: r.regeneration = s.regeneration; break;
            case AC: r.ac = s.ac; break;
            case SAVES: r.saves = s.saves; break;
            case MANEUVERS: r.maneuvers = s.maneuvers; break;
            case SKILLS: r.skills = s.skills; break;
            case ATTACK_ROUTINE: r.attackRoutine = s.attackRoutine; break;
            case NOTES: r.notes = s.notes; break;
            case CONDITIONS: r.conditions = new ArrayList<>(s.conditions); break;
            default: break;
        }
    }

    private static int indexOf(List<DebuffModel> list, String name) {
        String k = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < list.size(); i++) {
            if (key(list.get(i)).equals(k)) return i;
        }
        return -1;
    }

    private static String key(DebuffModel d) {
        return d.getName() == null ? "" : d.getName().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean same(DebuffModel a, DebuffModel b) {
        return Objects.equals(a.getName(), b.getName()) && a.getDuration() == b.getDuration()
                && Objects.equals(a.getDescription(), b.getDescription());
    }

    private static DebuffModel copy(DebuffModel d) {
        DebuffModel c = new DebuffModel(d.getName(), d.getDuration(), d.getDescription());
        c.copyCasterFrom(d);
        return c;
    }
}
