package com.rekijan.initiativetracker.ui.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.rekijan.initiativetracker.AppExtension;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.ui.fragments.CharacterDetailFragment;
import com.rekijan.initiativetracker.ui.fragments.MainActivityFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.initializeData(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, MainActivityFragment.newInstance())
                    .commit();

            if (getResources().getBoolean(R.bool.isTablet) && !app.getCharacterAdapter().getList().isEmpty()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0))
                        .commit();
            }
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(app.showBackNavigation());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //If done in fragment instead, return false

        //If not on tablet and a CharacterDetailFragment is present pop it from the stack before continuing
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        boolean fragmentExists = getSupportFragmentManager().getBackStackEntryCount() > 0;

        switch (item.getItemId()) {
            case R.id.action_settings_next_turn:
                if (!isTablet && fragmentExists) onSupportNavigateUp();
                nextTurn();
            return false;
            case R.id.action_settings_sort:
                if (!isTablet && fragmentExists) onSupportNavigateUp();
                return false;
            case R.id.action_settings_add_character:
                if (!isTablet && fragmentExists) onSupportNavigateUp();
                return false;
            case R.id.action_settings_delete_character:
                return false;
            case R.id.action_settings_about:
                return false;
//          case R.id.action_settings_select_party:
//                //TODO go to select party screen add in later
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void nextTurn() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0));
            transaction.commit();
        }
    }

    public void replaceCharacterDetailFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.commit();
        } else {
            transaction.replace(R.id.main_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.addToBackStack(null);
            transaction.commit();
            //Enable the back button in action bar
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            AppExtension app = (AppExtension) this.getApplicationContext();
            app.setShowBackNavigation(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        //Disable the back button in action bar only if it is the last fragment
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(getSupportFragmentManager().getBackStackEntryCount() > 1);
        getSupportFragmentManager().popBackStack();
        return true;
    }

}