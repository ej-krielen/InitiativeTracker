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
import java.util.List;

/**
 * A fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private List<CharacterModel> characters;
    private RecyclerView charactersRecyclerView;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        charactersRecyclerView = (RecyclerView) rootView.findViewById(R.id.characters_recyclerView);
        charactersRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        charactersRecyclerView.setLayoutManager(llm);

        initializeData();
        initializeAdapter();

        return rootView;
    }

    private void initializeData(){
        characters = new ArrayList<>();
        characters.add(new CharacterModel());
        characters.add(new CharacterModel());
        characters.add(new CharacterModel());
    }

    private void initializeAdapter(){
        CharacterAdapter adapter = new CharacterAdapter(characters);
        charactersRecyclerView.setAdapter(adapter);
    }

}
