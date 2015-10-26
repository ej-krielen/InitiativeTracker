package com.rekijan.initiativetracker.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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

}
