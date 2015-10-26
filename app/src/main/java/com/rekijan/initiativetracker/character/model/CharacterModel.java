package com.rekijan.initiativetracker.character.model;

import static com.rekijan.initiativetracker.AppConstants.DEBUFF;
import static com.rekijan.initiativetracker.AppConstants.HP;
import static com.rekijan.initiativetracker.AppConstants.INITIATIVE;

/**
 * Model class for the Character
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 11-10-2015
 */
public class CharacterModel {

    private int initiative = INITIATIVE;
    private int hp = HP;
    private int debuffTL = DEBUFF;
    private int debuffTC = DEBUFF;
    private int debuffTR = DEBUFF;
    private int debuffBL = DEBUFF;
    private int debuffBC = DEBUFF;
    private int debuffBR = DEBUFF;

    private String characterName = "";
    private String characterNotes = "";

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getDebuffTL() {
        return debuffTL;
    }

    public void setDebuffTL(int debuffTL) {
        this.debuffTL = debuffTL;
    }

    public int getDebuffTC() {
        return debuffTC;
    }

    public void setDebuffTC(int debuffTC) {
        this.debuffTC = debuffTC;
    }

    public int getDebuffTR() {
        return debuffTR;
    }

    public void setDebuffTR(int debuffTR) {
        this.debuffTR = debuffTR;
    }

    public int getDebuffBL() {
        return debuffBL;
    }

    public void setDebuffBL(int debuffBL) {
        this.debuffBL = debuffBL;
    }

    public int getDebuffBC() {
        return debuffBC;
    }

    public void setDebuffBC(int debuffBC) {
        this.debuffBC = debuffBC;
    }

    public int getDebuffBR() {
        return debuffBR;
    }

    public void setDebuffBR(int debuffBR) {
        this.debuffBR = debuffBR;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterNotes() {
        return characterNotes;
    }

    public void setCharacterNotes(String characterNotes) {
        this.characterNotes = characterNotes;
    }

}
