package com.rekijan.initiativetracker.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.feature.character.CharacterDefaults;
import com.rekijan.initiativetracker.feature.character.CharacterDetailFragment;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.encounter.EncounterFragment;
import com.rekijan.initiativetracker.feature.encounter.TurnDialogs;
import com.rekijan.initiativetracker.feature.info.InfoDialogs;
import com.rekijan.initiativetracker.feature.order.EditOrderFragment;
import com.rekijan.initiativetracker.monetization.BillingManager;

import java.util.List;

/**
 * The only activity. Hosts the encounter list and, on a phone, swaps it for the character
 * details or the Edit Order screen; on a tablet the details sit next to the list. It also handles
 * the toolbar menu and saves the fight when the app goes to the background.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class MainActivity extends AppCompatActivity {

    private InitiativeTrackerApp app;
    private Encounter encounter;
    private BillingManager billingManager;
    private boolean isTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setPopupTheme(R.style.PopupMenuStyle);

        app = (InitiativeTrackerApp) getApplication();
        encounter = app.getEncounter();
        isTablet = getResources().getBoolean(R.bool.isTablet);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.main_fragment_container, EncounterFragment.newInstance())
                    .commit();
            if (isTablet && encounter.size() > 0) {
                fm.beginTransaction()
                        .add(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0))
                        .commit();
            }
        }
        fm.addOnBackStackChangedListener(this::onBackStackChanged);
        onBackStackChanged();

        billingManager = new BillingManager(getApplication());
        billingManager.start();
    }

    @Override
    protected void onPause() {
        app.saveEncounter();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        billingManager.endConnection();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean editingOrder = getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container) instanceof EditOrderFragment;
        menu.setGroupVisible(R.id.group_encounter, !editingOrder);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings_next_turn) {
            closeDetailsOnPhone();
            nextTurn();
        } else if (id == R.id.action_settings_sort) {
            closeDetailsOnPhone();
            TurnDialogs.askRoundReset(this, encounter, encounter.sortByInitiative());
        } else if (id == R.id.action_settings_add_character) {
            closeDetailsOnPhone();
            encounter.add(CharacterDefaults.newCharacter(this));
        } else if (id == R.id.action_settings_delete_character) {
            InfoDialogs.howToDeleteCharacter(this);
        } else if (id == R.id.action_settings_set_pcs_max_hp) {
            showSkippedPcs(encounter.setPcsToMaxHp());
        } else if (id == R.id.action_settings_about) {
            InfoDialogs.about(this);
        } else if (id == R.id.action_settings_privacy) {
            InfoDialogs.privacyPolicy(this);
        } else if (id == R.id.action_settings_tip) {
            billingManager.openTipDialog(this);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    /** Shows a character's details: next to the list on a tablet, instead of it on a phone. */
    public void showCharacterDetail(int position) {
        if (isTablet) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position))
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, CharacterDetailFragment.newInstance(position))
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void showEditOrder() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, EditOrderFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    private void nextTurn() {
        Encounter.Turn turn = encounter.nextTurn();
        if (turn == null) return;
        TurnDialogs.onTurnStarted(this, encounter, turn);
        // On a tablet the details follow whoever's turn it is
        if (isTablet) showCharacterDetail(0);
    }

    /** Encounter actions from the menu go back to the list first on a phone. */
    private void closeDetailsOnPhone() {
        FragmentManager fm = getSupportFragmentManager();
        if (!isTablet && fm.getBackStackEntryCount() > 0) fm.popBackStack();
    }

    private void showSkippedPcs(List<String> names) {
        if (names.isEmpty()) return;
        String message = getString(R.string.toast_max_hp, TextUtils.join("\n", names));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void onBackStackChanged() {
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
        invalidateOptionsMenu();
    }
}
