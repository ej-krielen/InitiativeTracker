package com.rekijan.initiativetracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.character.adapter.CharacterAdapter;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.ui.activities.MainActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static com.rekijan.initiativetracker.AppConstants.GSON_TAG;
import static com.rekijan.initiativetracker.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetracker.AppConstants.SHARED_PREF_TAG;

/**
 * Extends Application to store and manipulate top-level data
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 16-5-2019
 */
public class AppExtension extends Application {

    private boolean showBackNavigation;
    private CharacterAdapter mCharacterAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        showBackNavigation = false;
    }

    /**
     * Looks for previously saved data and restores that, else it will add 5 empty characters
     *
     * @param activity Reference to {@link MainActivity} to pass to {@link CharacterAdapter}
     */
    public void initializeData(MainActivity activity) {
        if (mCharacterAdapter == null) {
            mCharacterAdapter = new CharacterAdapter(activity);

            SharedPreferences sharedPreferences = this.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = sharedPreferences.getString(GSON_TAG, null);
            Type type = new TypeToken<ArrayList<CharacterModel>>() {
            }.getType();
            ArrayList<CharacterModel> characters;
            characters = gson.fromJson(json, type);
            mCharacterAdapter.removeAll();
            if (characters != null) {
                for (CharacterModel c : characters) {
                    mCharacterAdapter.add(new CharacterModel(this, c.getInitiative(), c.getInitiativeBonus(), c.getSkills(), c.getAttackRoutine(), c.getAc(), c.getSaves(),
                            c.getManeuvers(), c.getHp(), c.getMaxHp(), c.getCharacterName(), c.getCharacterNotes(), c.isFirstRound(), c.isPC()));
                }
            } else {
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                for (CharacterModel c : mCharacterAdapter.getList()) {
                    c.setIsPC(true);
                }
            }
        }
    }

    /**
     * Saves all character data
     */
    public void saveData(int roundConter) {
        SharedPreferences sharedPreferences = this.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        ArrayList<CharacterModel> characters = mCharacterAdapter.getList();
        String json = gson.toJson(characters);
        editor.putString(GSON_TAG, json);
        editor.putInt(ROUND_COUNTER, roundConter);
        editor.apply();
    }

    public CharacterAdapter getCharacterAdapter() {
        return mCharacterAdapter;
    }

    public void setCharacterAdapter(CharacterAdapter mCharacterAdapter) {
        this.mCharacterAdapter = mCharacterAdapter;
    }

    public boolean showBackNavigation() {
        return showBackNavigation;
    }

    public void setShowBackNavigation(boolean showBackNavigation) {
        this.showBackNavigation = showBackNavigation;
    }

}