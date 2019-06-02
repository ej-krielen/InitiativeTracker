package com.rekijan.initiativetracker.character.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.widget.Toast;

import com.rekijan.initiativetracker.R;

import java.util.ArrayList;

import static com.rekijan.initiativetracker.AppConstants.AC_AND_SAVES;
import static com.rekijan.initiativetracker.AppConstants.HP;
import static com.rekijan.initiativetracker.AppConstants.INITIATIVE;

/**
 * Model class for the Character
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class CharacterModel implements Parcelable {

    private transient Context context; //transient to skip gson from trying to include it
    private long id;
    private long party_id;
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

    private String characterName;
    private String characterNotes;

    private ArrayList<DebuffModel> debuffList = new ArrayList<>();

    public CharacterModel(Context context) {
        initiative = INITIATIVE;
        initiativeBonus = INITIATIVE;
        skills = "";
        attackRoutine = "";
        boolean isTablet = context.getResources().getBoolean(R.bool.isTablet);
        ac = isTablet ? String.format(context.getString(R.string.standard_ac), AC_AND_SAVES) : String.format(context.getString(R.string.standard_mobile_ac), AC_AND_SAVES);
        saves = isTablet ? String.format(context.getString(R.string.standard_saves), AC_AND_SAVES) : String.format(context.getString(R.string.standard__mobile_saves), AC_AND_SAVES);
        maneuvers = String.format(context.getString(R.string.maneuvers), 0, AC_AND_SAVES);
        hp = HP;
        maxHp = HP;
        characterName = "";
        characterNotes = "";
        isFirstInRound = false;
        this.context = context;
    }

    /**
     * Called by the {@link com.rekijan.initiativetracker.character.adapter.CharacterAdapter} when its a characters turn <br>
     * Lowers all (de)buff values by one, but never to negative.
     */
    public void updateDebuffs() {
        for (DebuffModel d: debuffList) {
            int duration = d.getDuration();
            duration--;
            if (duration == 0) toastExpiredDebuffs(d.getName());
            if (duration < 0) duration = 0;
            d.setDuration(duration);
        }
    }

    private void toastExpiredDebuffs(String string) {
        Toast toast = Toast.makeText(context, String.format(context.getString(R.string.debuff_expired), string), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 350);
        toast.show();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getParty_id() {
        return party_id;
    }

    public void setParty_id(long party_id) {
        this.party_id = party_id;
    }

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int initiativeBonus) {
        this.initiativeBonus = initiativeBonus;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAttackRoutine() {
        return attackRoutine;
    }

    public void setAttackRoutine(String attackRoutine) {
        this.attackRoutine = attackRoutine;
    }

    public String getAc() { return ac; }

    public void setAc(String ac) { this.ac = ac; }

    public String getSaves() { return saves; }

    public void setSaves(String saves) { this.saves = saves; }

    public String getManeuvers() { return maneuvers; }

    public void setManeuvers(String maneuvers) { this.maneuvers = maneuvers; }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
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

    public Context getContext() { return context; }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setIsFirstRound(boolean isFirstInRound) {
        this.isFirstInRound = isFirstInRound;
    }

    public boolean isFirstRound() { return isFirstInRound; }

    public ArrayList<DebuffModel> getDebuffList() {
        //Need to check for null because previous iteration didn't have this array list
        if (debuffList == null) debuffList = new ArrayList<>();
        return debuffList;
    }

    public void setDebuffList(ArrayList<DebuffModel> debuffList) {
        this.debuffList = debuffList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.party_id);
        dest.writeInt(this.initiative);
        dest.writeInt(this.initiativeBonus);
        dest.writeString(this.skills);
        dest.writeString(this.attackRoutine);
        dest.writeString(this.ac);
        dest.writeString(this.saves);
        dest.writeString(this.maneuvers);
        dest.writeInt(this.hp);
        dest.writeInt(this.maxHp);
        dest.writeByte(this.isFirstInRound ? (byte) 1 : (byte) 0);
        dest.writeString(this.characterName);
        dest.writeString(this.characterNotes);
        dest.writeTypedList(this.debuffList);
    }

    protected CharacterModel(Parcel in) {
        this.id = in.readLong();
        this.party_id = in.readLong();
        this.initiative = in.readInt();
        this.initiativeBonus = in.readInt();
        this.skills = in.readString();
        this.attackRoutine = in.readString();
        this.ac = in.readString();
        this.saves = in.readString();
        this.maneuvers = in.readString();
        this.hp = in.readInt();
        this.maxHp = in.readInt();
        this.isFirstInRound = in.readByte() != 0;
        this.characterName = in.readString();
        this.characterNotes = in.readString();
        this.debuffList = in.createTypedArrayList(DebuffModel.CREATOR);
    }

    public static final Parcelable.Creator<CharacterModel> CREATOR = new Parcelable.Creator<CharacterModel>() {
        @Override
        public CharacterModel createFromParcel(Parcel source) {
            return new CharacterModel(source);
        }

        @Override
        public CharacterModel[] newArray(int size) {
            return new CharacterModel[size];
        }
    };
}