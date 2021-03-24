package com.rekijan.initiativetracker.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
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

        final AppExtension app = (AppExtension) getActivity().getApplicationContext();
        final EditOrderAdapter adapter = new EditOrderAdapter(requireActivity());
        adapter.addAll(app.getCharacterAdapter().getList());
        editOrderRecyclerView.setAdapter(adapter);

        //Setup long click listener to set character as first in the round
        editOrderRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), editOrderRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) { }

            @Override
            public void onLongClick(View view, final int position) {
                app.getCharacterAdapter().setFirstInRound(position);
                adapter.notifyDataSetChanged();
            }
        }));

        return rootView;
    }

    /* BEGIN REGION Needed to setup long click listener on the RecyclerView */
    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private EditOrderFragment.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final EditOrderFragment.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }

    /* END REGION setup long click listener */
}