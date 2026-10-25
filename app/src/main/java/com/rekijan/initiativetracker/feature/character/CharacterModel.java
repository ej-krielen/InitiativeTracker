package com.rekijan.initiativetracker.feature.character;

import com.rekijan.initiativetracker.core.util.NumberInput;

import java.util.ArrayList;
import java.util.List;

/**
 * One combatant in the initiative order: a PC or an NPC/monster with initiative, hit points,
 * healing per turn, free-text stat lines and a list of (de)buffs. Plain Java (no Android), so the
 * turn rules can be unit tested.
 * <p>
 * The field names are part of the JSON that 1.x saved in SharedPreferences. They must stay the
 * same so users keep their party after the update.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class CharacterModel {

    public static final int MIN_INITIATIVE = -99;
    public static final int MAX_INITIATIVE = 999;
    public static final int MIN_HP = -99999;
    public static final int MAX_HP = 99999;
    public static final int MAX_HEALING = 999;

    private int initiative;
    private int initiativeBonus;
    private String skills;
    private String attackRoutine;
    private String ac;
    private String saves;
    private String maneuvers;
    private int hp;
    private int maxHp;
    private boolean isFirstInRound;
    private boolean isPC;
    private int fastHealing;
    private int regeneration;
    private String characterName;
    private String characterNotes;
    private ArrayList<DebuffModel> debuffList = new ArrayList<>();
    /** Firebase uid of the player who sent this character with Table sync; null for everyone else. */
    private String syncId;

    /** Empty character; also used by Gson when loading saved data. */
    public CharacterModel() {
        skills = "";
        attackRoutine = "";
        ac = "";
        saves = "";
        maneuvers = "";
        characterName = "";
        characterNotes = "";
    }

    /**
     * Lowers every (de)buff by one round, never below 0.
     *
     * @return The (de)buffs that just reached 0 this turn, so the user can be asked to remove them.
     */
    public List<DebuffModel> tickDebuffs() {
        List<DebuffModel> expired = new ArrayList<>();
        for (DebuffModel d : getDebuffList()) {
            int duration = d.getDuration() - 1;
            if (duration == 0) expired.add(d);
            d.setDuration(duration);
        }
        return expired;
    }

    /** Adds healing (fast healing, regeneration) to the current hp. */
    public void heal(int amount) {
        setHp(hp + amount);
    }

    public boolean isAboveMaxHp() {
        return hp > maxHp;
    }

    public void capHpAtMaxHp() {
        if (hp > maxHp) hp = maxHp;
    }

    /** An NPC below 0 hp is probably dead, so the app offers to remove it. PCs never are. */
    public boolean shouldOfferRemoval() {
        return !isPC && hp < 0;
    }

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = NumberInput.clamp(initiative, MIN_INITIATIVE, MAX_INITIATIVE);
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int initiativeBonus) {
        this.initiativeBonus = NumberInput.clamp(initiativeBonus, MIN_INITIATIVE, MAX_INITIATIVE);
    }

    public String getSkills() {
        return skills == null ? "" : skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAttackRoutine() {
        return attackRoutine == null ? "" : attackRoutine;
    }

    public void setAttackRoutine(String attackRoutine) {
        this.attackRoutine = attackRoutine;
    }

    public String getAc() {
        return ac == null ? "" : ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public String getSaves() {
        return saves == null ? "" : saves;
    }

    public void setSaves(String saves) {
        this.saves = saves;
    }

    public String getManeuvers() {
        return maneuvers == null ? "" : maneuvers;
    }

    public void setManeuvers(String maneuvers) {
        this.maneuvers = maneuvers;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = NumberInput.clamp(hp, MIN_HP, MAX_HP);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = NumberInput.clamp(maxHp, 0, MAX_HP);
    }

    public String getCharacterName() {
        return characterName == null ? "" : characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterNotes() {
        return characterNotes == null ? "" : characterNotes;
    }

    public void setCharacterNotes(String characterNotes) {
        this.characterNotes = characterNotes;
    }

    public boolean isFirstInRound() {
        return isFirstInRound;
    }

    public void setFirstInRound(boolean firstInRound) {
        this.isFirstInRound = firstInRound;
    }

    public boolean isPC() {
        return isPC;
    }

    public void setPC(boolean isPC) {
        this.isPC = isPC;
    }

    public int getFastHealing() {
        return fastHealing;
    }

    public void setFastHealing(int fastHealing) {
        this.fastHealing = NumberInput.clamp(fastHealing, 0, MAX_HEALING);
    }

    public int getRegeneration() {
        return regeneration;
    }

    public void setRegeneration(int regeneration) {
        this.regeneration = NumberInput.clamp(regeneration, 0, MAX_HEALING);
    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public List<DebuffModel> getDebuffList() {
        // Saves from before (de)buffs existed have no list
        if (debuffList == null) debuffList = new ArrayList<>();
        return debuffList;
    }
}
