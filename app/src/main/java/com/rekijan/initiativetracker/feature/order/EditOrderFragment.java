package com.rekijan.initiativetracker.feature.order;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.RecyclerLongPressListener;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

/**
 * Screen to change the initiative order by hand without sorting. Long press a character to make
 * them first in round.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class EditOrderFragment extends Fragment implements Encounter.Listener {

    private Encounter encounter;
    private EditOrderAdapter adapter;

    public static EditOrderFragment newInstance() {
        return new EditOrderFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        encounter = ((InitiativeTrackerApp) requireActivity().getApplication()).getEncounter();

        RecyclerView recyclerView = view.findViewById(R.id.edit_order_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EditOrderAdapter(encounter);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                encounter::setFirstInRound));

        encounter.addListener(this);
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // Swaps and the first-in-round marker touch several rows
    @Override
    public void onEncounterChanged() {
        adapter.notifyDataSetChanged();
    }
}
