package com.rekijan.initiativetracker.character.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.fragment.app.FragmentActivity;

import com.rekijan.initiativetracker.AppExtension;
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
    private boolean isPC;
    private int fastHealing;
    private int regeneration;

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
        isPC = false;
        fastHealing = 0;
        regeneration = 0;
        this.context = context;
    }

    public CharacterModel(Context context, int initiative, int initiativeBonus, String skills, String attackRoutine, String ac, String saves, String maneuvers, int hp, int maxHp, String characterName, String characterNotes, boolean isFirstInRound, boolean isPC, int fastHealing, int regeneration) {
        this.initiative = initiative;
        this.initiativeBonus = initiativeBonus;
        this.skills = skills;
        this.attackRoutine = attackRoutine;
        this.ac = ac;
        this.saves = saves;
        this.maneuvers = maneuvers;
        this.hp = hp;
        this.maxHp = maxHp;
        this.characterName = characterName;
        this.characterNotes = characterNotes;
        this.isFirstInRound = isFirstInRound;
        this.isPC = isPC;
        this.fastHealing = fastHealing;
        this.regeneration = regeneration;
        this.context = context;
    }

    /**
     * Called by the {@link com.rekijan.initiativetracker.character.adapter.CharacterAdapter} when its a characters turn <br>
     * Lowers all (de)buff values by one, but never to negative. <br>
     * Checks when a debuff goes to 0 duration for the first time and asks user if they want to remove those.
     * @param activity
     */
    public void updateDebuffs(FragmentActivity activity) {
        int debuffPosition = 0;
        for (DebuffModel d: debuffList) {
            int duration = d.getDuration();
            duration--;
            if (duration == 0) askRoundResetConfirmation(activity, d, debuffPosition);
            if (duration < 0) duration = 0;
            d.setDuration(duration);
            debuffPosition++;
        }
        this.setDebuffList(debuffList);
    }

    /**
     * Create a dialog so the user can choose to keep or remove a debuff that just went to 0 duration
     * @param activity used to create dialog
     * @param debuff Debuff that has just been expired
     * @param debuffPosition position in list for debuff
     */
    private void askRoundResetConfirmation(FragmentActivity activity, DebuffModel debuff, final int debuffPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);
        builder.setMessage(String.format(context.getString(R.string.debuff_expired), debuff.getName()))
                .setTitle(activity.getString(R.string.dialog_debuff_expired_title));
        builder.setPositiveButton(activity.getString(R.string.dialog_debuff_remove), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                debuffList.remove(debuffPosition);
            }
        });
        builder.setNegativeButton(activity.getString(R.string.dialog_debuff_keep), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void automaticHealingCheck(final FragmentActivity activity) {
        if (fastHealing > 0)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_fast_healing_message))
                    .setTitle(activity.getString(R.string.auto_healing_dialog_fast_healing_title));
            builder.setPositiveButton(activity.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addAutomaticHealing(fastHealing, activity);
                }
            });
            builder.setNegativeButton(activity.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        if (regeneration > 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_regeneration_message))
                    .setTitle(activity.getString(R.string.auto_healing_dialog_regeneration_title));
            builder.setPositiveButton(activity.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addAutomaticHealing(regeneration, activity);
                }
            });
            builder.setNegativeButton(activity.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void addAutomaticHealing(int healing, FragmentActivity activity) {
        final AppExtension mApp = (AppExtension) activity.getApplicationContext();
        setHp(hp+healing);
        mApp.getCharacterAdapter().notifyDataSetChanged();
        if (hp > maxHp) {
            resolveExceededMaxHp(activity);
        }
    }

    private void resolveExceededMaxHp(final FragmentActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.auto_healing_dialog_max_exceeded_message))
                .setTitle(activity.getString(R.string.auto_healing_dialog_max_exceeded_title));
        builder.setPositiveButton(activity.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final AppExtension mApp = (AppExtension) activity.getApplicationContext();
                hp = maxHp;
                mApp.getCharacterAdapter().notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(activity.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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

    public void setIsPC(boolean isPC) {
        this.isPC = isPC;
    }

    public boolean isPC() { return isPC; }

    public void setFastHealing(int fastHealing) {
        this.fastHealing = fastHealing;
    }

    public int getFastHealing() {
        return fastHealing;
    }

    public void setRegeneration(int regeneration) {
        this.regeneration = regeneration;
    }

    public int getRegeneration() {
        return regeneration;
    }

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
        dest.writeByte(this.isPC ? (byte) 1 : (byte) 0);
        dest.writeString(this.characterName);
        dest.writeString(this.characterNotes);
        dest.writeTypedList(this.debuffList);
        dest.writeInt(this.fastHealing);
        dest.writeInt(this.regeneration);
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
        this.isPC = in.readByte() != 0;
        this.characterName = in.readString();
        this.characterNotes = in.readString();
        this.debuffList = in.createTypedArrayList(DebuffModel.CREATOR);
        this.fastHealing = in.readInt();
        this.regeneration = in.readInt();
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