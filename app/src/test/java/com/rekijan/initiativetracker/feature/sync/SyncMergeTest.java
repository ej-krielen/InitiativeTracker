package com.rekijan.initiativetracker.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetracker.feature.character.CharacterModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests {@link SyncMerge}: a new player is added once, a resend updates the same character, an
 * unchanged sheet never undoes the GM's edits, and a deleted character only comes back on a new send.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncMergeTest {

    private final List<CharacterModel> party = new ArrayList<>();
    private final Map<String, Long> applied = new HashMap<>();

    private SyncMerge.Result merge(String uid, PlayerSheet sheet) {
        Map<String, PlayerSheet> received = new LinkedHashMap<>();
        received.put(uid, sheet);
        SyncMerge.Result result = SyncMerge.merge(party, applied, received);
        party.addAll(result.added);
        return result;
    }

    @Test
    public void newPlayerIsAddedAsPc() {
        party.add(new CharacterModel());
        SyncMerge.Result result = merge("uid1", PlayerSheetTest.valeros());
        assertEquals(1, result.added.size());
        CharacterModel added = result.added.get(0);
        assertEquals("uid1", added.getSyncId());
        assertEquals("Valeros", added.getCharacterName());
        assertTrue(added.isPC());
        assertEquals(1000L, (long) applied.get("uid1"));
    }

    @Test
    public void resendUpdatesTheSameCharacter() {
        merge("uid1", PlayerSheetTest.valeros());
        CharacterModel first = party.get(0);
        PlayerSheet resend = PlayerSheetTest.valeros();
        resend.hp = 12;
        resend.sentAt = 2000;
        SyncMerge.Result result = merge("uid1", resend);
        assertTrue(result.added.isEmpty());
        assertEquals(1, result.updated.size());
        assertEquals(1, party.size());
        assertSame(first, party.get(0));
        assertEquals(12, first.getHp());
    }

    @Test
    public void unchangedSheetKeepsTheGmsEdits() {
        merge("uid1", PlayerSheetTest.valeros());
        party.get(0).setHp(5); // the GM applied damage
        SyncMerge.Result result = merge("uid1", PlayerSheetTest.valeros());
        assertFalse(result.hasChanges());
        assertEquals(5, party.get(0).getHp());
    }

    @Test
    public void olderSheetIsIgnored() {
        merge("uid1", PlayerSheetTest.valeros());
        PlayerSheet older = PlayerSheetTest.valeros();
        older.sentAt = 500;
        older.hp = 1;
        assertFalse(merge("uid1", older).hasChanges());
        assertEquals(30, party.get(0).getHp());
    }

    @Test
    public void deletedCharacterComesBackOnlyOnANewSend() {
        merge("uid1", PlayerSheetTest.valeros());
        party.clear(); // the GM deleted the character
        assertFalse(merge("uid1", PlayerSheetTest.valeros()).hasChanges());
        assertTrue(party.isEmpty());

        PlayerSheet resend = PlayerSheetTest.valeros();
        resend.sentAt = 3000;
        assertEquals(1, merge("uid1", resend).added.size());
        assertEquals(1, party.size());
    }

    @Test
    public void twoPlayersGetTwoCharacters() {
        Map<String, PlayerSheet> received = new LinkedHashMap<>();
        received.put("uid1", PlayerSheetTest.valeros());
        PlayerSheet seoni = PlayerSheetTest.valeros();
        seoni.name = "Seoni";
        received.put("uid2", seoni);
        assertEquals(2, SyncMerge.merge(party, applied, received).added.size());
    }

    @Test
    public void localCharactersAreNeverTouched() {
        CharacterModel local = new CharacterModel();
        local.setCharacterName("Goblin");
        party.add(local);
        merge("uid1", PlayerSheetTest.valeros());
        assertEquals("Goblin", local.getCharacterName());
        assertEquals(2, party.size());
    }
}
