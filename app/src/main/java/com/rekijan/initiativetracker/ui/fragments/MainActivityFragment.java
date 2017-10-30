package com.rekijan.initiativetracker.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.adapter.CharacterAdapter;
import com.rekijan.initiativetracker.character.model.CharacterModel;

import java.util.ArrayList;

/**
 * A fragment containing the list of CharacterModels
 */
public class MainActivityFragment extends Fragment {

    private CharacterAdapter mAdapter = new CharacterAdapter();

    public MainActivityFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        RecyclerView charactersRecyclerView = (RecyclerView) rootView.findViewById(R.id.characters_recyclerView);
        charactersRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        charactersRecyclerView.setLayoutManager(llm);
        charactersRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    private void initializeData() {
        mAdapter.add(new CharacterModel());
        mAdapter.add(new CharacterModel());
        mAdapter.add(new CharacterModel());
    }

    private void addCharacter() {
        mAdapter.add(new CharacterModel());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("characters", mAdapter.getList());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<CharacterModel> list = savedInstanceState.getParcelableArrayList("characters");
            if (list != null) {
                mAdapter.addAll(list);
            }
        } else {
            initializeData();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings_next_turn:
                //TODO go to select party screen
                return true;
            case R.id.action_settings_sort:
                mAdapter.sortInitiative();
                return true;
            case R.id.action_settings_add_character:
                addCharacter();
                return true;
//          case R.id.action_settings_select_party:
//                //TODO go to select party screen add in later
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
