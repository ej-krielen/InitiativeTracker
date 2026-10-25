package com.rekijan.initiativetracker.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
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
 * The only activity. Hosts the encounter list and, in portrait, swaps it for the character
 * details or the Edit Order screen; in landscape (layout-land) the details sit next to the list.
 * It also handles the toolbar menu and saves the fight when the app goes to the background.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class MainActivity extends AppCompatActivity {

    private InitiativeTrackerApp app;
    private Encounter encounter;
    private BillingManager billingManager;
    private boolean isTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (InitiativeTrackerApp) getApplication();
        encounter = app.getEncounter();
        isTwoPane = findViewById(R.id.second_fragment_container) != null;

        setUpPanes(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);
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
            closeDetailsInOnePane();
            nextTurn();
        } else if (id == R.id.action_settings_sort) {
            closeDetailsInOnePane();
            TurnDialogs.askSort(this, encounter);
        } else if (id == R.id.action_settings_add_character) {
            closeDetailsInOnePane();
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

    /** Shows a character's details: next to the list in landscape, instead of it in portrait. */
    public void showCharacterDetail(int position) {
        if (isTwoPane) {
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

    /**
     * Puts the encounter in the main pane and, in landscape, the details of the first character
     * next to it. Rotating switches between one and two panes, so the details move along.
     */
    private void setUpPanes(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.main_fragment_container, EncounterFragment.newInstance())
                    .commit();
            if (isTwoPane && encounter.size() > 0) showCharacterDetail(0);
            return;
        }

        Fragment detailPane = fm.findFragmentById(R.id.second_fragment_container);
        if (!isTwoPane) {
            // Back to portrait: the second pane is gone
            if (detailPane != null) fm.beginTransaction().remove(detailPane).commitNow();
            return;
        }
        Fragment mainPane = fm.findFragmentById(R.id.main_fragment_container);
        if (mainPane instanceof CharacterDetailFragment) {
            // To landscape with the details open: list on the left, the same details on the right
            int position = ((CharacterDetailFragment) mainPane).getPosition();
            fm.popBackStackImmediate();
            showCharacterDetail(position);
        } else if (detailPane == null && encounter.size() > 0) {
            showCharacterDetail(0);
        }
    }

    private void nextTurn() {
        Encounter.Turn turn = encounter.nextTurn();
        if (turn == null) return;
        TurnDialogs.onTurnStarted(this, encounter, turn);
        // In two panes the details follow whoever's turn it is
        if (isTwoPane) showCharacterDetail(0);
    }

    /** Encounter actions from the menu go back to the list first when there is one pane. */
    private void closeDetailsInOnePane() {
        FragmentManager fm = getSupportFragmentManager();
        if (!isTwoPane && fm.getBackStackEntryCount() > 0) fm.popBackStack();
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

    /**
     * Edge-to-edge: pads the whole screen for the status bar, navigation bar and display cutout,
     * and for the keyboard when it is open. Same as Mook Roller.
     */
    private static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
