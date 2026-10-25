package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;

import com.rekijan.initiativetracker.feature.character.DebuffModel;

import org.junit.Test;

/**
 * Tests {@link SheetMerge}: the GM's changes come in, the player's unsent edits stay, and the
 * GM's answer to a send puts refused values back.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SheetMergeTest {

    private static PlayerSheet sheet(int hp, String notes) {
        PlayerSheet s = new PlayerSheet();
        s.name = "Valeros";
        s.hp = hp;
        s.maxHp = 34;
        s.notes = notes;
        return s;
    }

    @Test
    public void gmChangesComeInWhenThePlayerDidntTouchThem() {
        PlayerSheet base = sheet(30, "");
        PlayerSheet merged = SheetMerge.merge(base, sheet(30, ""), sheet(18, ""));
        assertEquals(18, merged.hp);
    }

    @Test
    public void unsentPlayerEditsStay() {
        PlayerSheet base = sheet(30, "");
        PlayerSheet local = sheet(30, "Cleave next turn");
        PlayerSheet merged = SheetMerge.merge(base, local, sheet(18, ""));
        assertEquals(18, merged.hp);
        assertEquals("Cleave next turn", merged.notes);
    }

    @Test
    public void refusedValuesGoBackWhenTheBaseIsWhatWasSent() {
        PlayerSheet sent = sheet(30, "Power attack");
        PlayerSheet gmAnswer = sheet(20, "Power attack"); // HP refused, notes accepted
        PlayerSheet merged = SheetMerge.merge(sent, sent.cleaned(), gmAnswer);
        assertEquals(20, merged.hp);
        assertEquals("Power attack", merged.notes);
    }

    @Test
    public void editsAfterSendingSurviveTheAnswer() {
        PlayerSheet sent = sheet(30, "");
        PlayerSheet local = sheet(30, "Typed while waiting");
        PlayerSheet merged = SheetMerge.merge(sent, local, sheet(20, ""));
        assertEquals(20, merged.hp);
        assertEquals("Typed while waiting", merged.notes);
    }

    @Test
    public void conditionsFollowTheGmUnlessThePlayerChangedThem() {
        PlayerSheet base = sheet(30, "");
        PlayerSheet remote = sheet(30, "");
        remote.conditions.add(new DebuffModel("Shaken", 2, ""));

        assertEquals(1, SheetMerge.merge(base, sheet(30, ""), remote).conditions.size());

        PlayerSheet local = sheet(30, "");
        local.conditions.add(new DebuffModel("Haste", 3, ""));
        PlayerSheet merged = SheetMerge.merge(base, local, remote);
        assertEquals(1, merged.conditions.size());
        assertEquals("Haste", merged.conditions.get(0).getName());
    }
}
