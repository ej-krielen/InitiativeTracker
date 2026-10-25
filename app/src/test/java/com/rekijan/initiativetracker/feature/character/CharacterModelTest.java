package com.rekijan.initiativetracker.feature.character;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Tests {@link CharacterModel} and {@link DebuffModel}: (de)buff timers, healing, the removal
 * prompt and the allowed number ranges.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class CharacterModelTest {

    @Test
    public void debuffWithoutTimerNeverExpiresAgain() {
        CharacterModel c = new CharacterModel();
        DebuffModel noTimer = new DebuffModel("Cursed", 0, "");
        c.getDebuffList().add(noTimer);

        assertTrue(c.tickDebuffs().isEmpty());
        assertEquals(0, noTimer.getDuration());
    }

    @Test
    public void debuffExpiresExactlyOnce() {
        CharacterModel c = new CharacterModel();
        c.getDebuffList().add(new DebuffModel("Haste", 2, ""));

        assertTrue(c.tickDebuffs().isEmpty());
        List<DebuffModel> expired = c.tickDebuffs();
        assertEquals(1, expired.size());
        assertTrue(c.tickDebuffs().isEmpty());
    }

    @Test
    public void healingCanGoAboveMaxUntilCapped() {
        CharacterModel c = new CharacterModel();
        c.setMaxHp(20);
        c.setHp(18);
        c.heal(5);
        assertEquals(23, c.getHp());
        assertTrue(c.isAboveMaxHp());
        c.capHpAtMaxHp();
        assertEquals(20, c.getHp());
        assertFalse(c.isAboveMaxHp());
    }

    @Test
    public void onlyNpcsBelowZeroAreOfferedForRemoval() {
        CharacterModel npc = new CharacterModel();
        npc.setHp(-1);
        assertTrue(npc.shouldOfferRemoval());

        npc.setHp(0);
        assertFalse(npc.shouldOfferRemoval());

        CharacterModel pc = new CharacterModel();
        pc.setPC(true);
        pc.setHp(-10);
        assertFalse(pc.shouldOfferRemoval());
    }

    @Test
    public void numbersStayInsideTheirRanges() {
        CharacterModel c = new CharacterModel();
        c.setInitiative(5000);
        assertEquals(CharacterModel.MAX_INITIATIVE, c.getInitiative());
        c.setInitiative(-5000);
        assertEquals(CharacterModel.MIN_INITIATIVE, c.getInitiative());
        c.setHp(Integer.MAX_VALUE);
        assertEquals(CharacterModel.MAX_HP, c.getHp());
        c.setMaxHp(-3);
        assertEquals(0, c.getMaxHp());
        c.setFastHealing(-1);
        assertEquals(0, c.getFastHealing());
        c.setRegeneration(100000);
        assertEquals(CharacterModel.MAX_HEALING, c.getRegeneration());

        DebuffModel d = new DebuffModel("Long", 100000, "");
        assertEquals(DebuffModel.MAX_DURATION, d.getDuration());
        d.setDuration(-2);
        assertEquals(0, d.getDuration());
    }

    @Test
    public void healingDoesNotOverflow() {
        CharacterModel c = new CharacterModel();
        c.setHp(CharacterModel.MAX_HP);
        c.heal(CharacterModel.MAX_HEALING);
        assertEquals(CharacterModel.MAX_HP, c.getHp());
    }
}
