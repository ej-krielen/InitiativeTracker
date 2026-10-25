package com.rekijan.initiativetracker.feature.sync;

import java.util.ArrayList;

/**
 * Puts a new version from the GM on the player's sheet without losing what the player is still
 * typing. Per field it compares three versions: the base (what the player last had from the GM,
 * or what they sent when this is the GM's answer to that send), the player's sheet now, and the
 * GM's new version:
 * <ul>
 *     <li>The player didn't touch the field since the base: the GM's value is taken.</li>
 *     <li>The player changed it: their value stays, marked as not sent yet.</li>
 * </ul>
 * The conditions count as one field here: when the player changed any condition, their whole
 * list stays until they send it. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class SheetMerge {

    private SheetMerge() {
    }

    /** @return The merged sheet; sentAt is the player's. */
    public static PlayerSheet merge(PlayerSheet base, PlayerSheet local, PlayerSheet remote) {
        PlayerSheet b = base.cleaned();
        PlayerSheet l = local.cleaned();
        PlayerSheet r = remote.cleaned();
        PlayerSheet m = l.cleaned();
        m.name = pick(b.name, l.name, r.name);
        m.initiative = pick(b.initiative, l.initiative, r.initiative);
        m.initiativeBonus = pick(b.initiativeBonus, l.initiativeBonus, r.initiativeBonus);
        m.hp = pick(b.hp, l.hp, r.hp);
        m.maxHp = pick(b.maxHp, l.maxHp, r.maxHp);
        m.fastHealing = pick(b.fastHealing, l.fastHealing, r.fastHealing);
        m.regeneration = pick(b.regeneration, l.regeneration, r.regeneration);
        m.ac = pick(b.ac, l.ac, r.ac);
        m.saves = pick(b.saves, l.saves, r.saves);
        m.maneuvers = pick(b.maneuvers, l.maneuvers, r.maneuvers);
        m.skills = pick(b.skills, l.skills, r.skills);
        m.attackRoutine = pick(b.attackRoutine, l.attackRoutine, r.attackRoutine);
        m.notes = pick(b.notes, l.notes, r.notes);
        m.conditions = SyncDiff.sameConditions(l.conditions, b.conditions)
                ? new ArrayList<>(r.conditions) : new ArrayList<>(l.conditions);
        return m;
    }

    private static <T> T pick(T base, T local, T remote) {
        return local.equals(base) ? remote : local;
    }
}
