package com.rekijan.initiativetracker.character.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.widget.Toast;

import com.rekijan.initiativetracker.R;

import static com.rekijan.initiativetracker.AppConstants.DEBUFF;
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
    private int hp;
    private int debuffTL;
    private int debuffTC;
    private int debuffTR;
    private int debuffBL;
    private int debuffBC;
    private int debuffBR;

    private String characterName;
    private String characterNotes;

    public CharacterModel(Context context) {
        initiative = INITIATIVE;
        hp = HP;
        debuffTL = DEBUFF;
        debuffTC = DEBUFF;
        debuffTR = DEBUFF;
        debuffBL = DEBUFF;
        debuffBC = DEBUFF;
        debuffBR = DEBUFF;
        characterName = "";
        characterNotes = "";
        this.context = context;
    }

    /**
     * Called by the {@link com.rekijan.initiativetracker.character.adapter.CharacterAdapter} when its a characters turn <br>
     * Lowers all (de)buff values by one, but never to negative.
     */
    public void updateDebuffs() {
        debuffTL--;
        if (debuffTL == 0) toastExpiredDebuffs(context.getString(R.string.debuff_top_left));
        if (debuffTL < 0) debuffTL = 0;
        debuffTC--;
        if (debuffTC == 0) toastExpiredDebuffs(context.getString(R.string.debuff_middle_top));
        if (debuffTC < 0) debuffTC = 0;
        debuffTR--;
        if (debuffTR == 0) toastExpiredDebuffs(context.getString(R.string.debuff_top_right));
        if (debuffTR < 0) debuffTR = 0;
        debuffBL--;
        if (debuffBL == 0) toastExpiredDebuffs(context.getString(R.string.debuff_bottom_left));
        if (debuffBL < 0) debuffBL = 0;
        debuffBC--;
        if (debuffBC == 0) toastExpiredDebuffs(context.getString(R.string.debuff_middle_bottom));
        if (debuffBC < 0) debuffBC = 0;
        debuffBR--;
        if (debuffBR == 0) toastExpiredDebuffs(context.getString(R.string.debuff_bottom_right));
        if (debuffBR < 0) debuffBR = 0;
    }

    private void toastExpiredDebuffs(String string) {
        Toast toast = Toast.makeText(context, String.format(context.getString(R.string.debuff_expired), string), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 350);
        toast.show();
    }

    public static final Parcelable.Creator<CharacterModel> CREATOR
            = new Parcelable.Creator<CharacterModel>() {
        public CharacterModel createFromParcel(Parcel in) {
            return new CharacterModel(in);
        }

        public CharacterModel[] newArray(int size) {
            return new CharacterModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(party_id);
        dest.writeInt(initiative);
        dest.writeInt(hp);
        dest.writeInt(debuffTL);
        dest.writeInt(debuffTC);
        dest.writeInt(debuffTR);
        dest.writeInt(debuffBL);
        dest.writeInt(debuffBC);
        dest.writeInt(debuffBR);
        dest.writeString(characterName);
        dest.writeString(characterNotes);
    }

    private CharacterModel(Parcel in) {
        id = in.readLong();
        party_id = in.readLong();
        initiative = in.readInt();
        hp = in.readInt();
        debuffTL = in.readInt();
        debuffTC = in.readInt();
        debuffTR = in.readInt();
        debuffBL = in.readInt();
        debuffBC = in.readInt();
        debuffBR = in.readInt();
        characterName = in.readString();
        characterNotes = in.readString();

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

    public Context getContext() { return context; }
}