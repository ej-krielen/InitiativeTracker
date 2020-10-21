package com.rekijan.initiativetracker.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.AppExtension;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.adapter.EditOrderAdapter;

public class EditOrderFragment extends Fragment {

    public EditOrderFragment() {
        // Required empty public constructor
    }

    public static EditOrderFragment newInstance() {
        EditOrderFragment fragment = new EditOrderFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        menu.clear();
        inflater.inflate(R.menu.menu_edit_order,menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_edit_order, container, false);

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView editOrderRecyclerView = rootView.findViewById(R.id.edit_order_recyclerView);
        editOrderRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        editOrderRecyclerView.setLayoutManager(llm);

        AppExtension app = (AppExtension) getActivity().getApplicationContext();
        final EditOrderAdapter adapter = new EditOrderAdapter(requireActivity());
        adapter.addAll(app.getCharacterAdapter().getList());
        editOrderRecyclerView.setAdapter(adapter);

        return rootView;
    }
}