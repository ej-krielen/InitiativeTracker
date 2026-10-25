package com.rekijan.initiativetracker.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.rekijan.initiativetracker.BuildConfig;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.feature.character.CharacterDefaults;
import com.rekijan.initiativetracker.feature.character.CharacterDetailFragment;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.encounter.EncounterFragment;
import com.rekijan.initiativetracker.feature.encounter.TurnDialogs;
import com.rekijan.initiativetracker.feature.info.InfoDialogs;
import com.rekijan.initiativetracker.feature.order.EditOrderFragment;
import com.rekijan.initiativetracker.feature.party.Party;
import com.rekijan.initiativetracker.feature.party.PartyBook;
import com.rekijan.initiativetracker.feature.party.PartyDialogs;
import com.rekijan.initiativetracker.feature.party.PartyNames;
import com.rekijan.initiativetracker.feature.party.PartySlots;
import com.rekijan.initiativetracker.monetization.BillingManager;

import java.util.List;

import nl.rekijan.ads.AdConsentManager;
import nl.rekijan.ads.RekijanBannerAdView;

/**
 * The only activity. Hosts the encounter list and, in portrait, swaps it for the character
 * details or the Edit Order screen; in landscape (layout-land) the details sit next to the list.
 * The toolbar holds the encounter actions; the navigation drawer ({@link NavDrawer}) holds the
 * parties and the app items. It also runs the party actions, the ad consent flow and the ad bar,
 * billing (tips, Remove Ads, party packs), and saves the fight when the app goes to the background.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class MainActivity extends AppCompatActivity implements NavDrawer.Listener {

    private static final String LOG_TAG = "MainActivity";

    private InitiativeTrackerApp app;
    private Encounter encounter;
    private BillingManager billingManager;
    private RekijanBannerAdView adBar;
    private AdConsentManager adConsentManager;
    private MaterialToolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavDrawer navDrawer;
    private boolean isTwoPane;

    /**
     * Reacts to the consent outcome, at startup and after the user changed their choice in the
     * privacy settings form. Updates the drawer so "Privacy Settings" appears when required.
     */
    private final AdConsentManager.Callback adCallback = new AdConsentManager.Callback() {
        @Override
        public void onReady() {
            if (isFinishing() || isDestroyed()) return;
            if (!app.isAdsRemoved()) { // may have been bought while consent was loading
                adBar.load(BuildConfig.ADMOB_BANNER_ID);
            }
            refreshDrawer();
        }

        @Override
        public void onFailure(@NonNull String reason) {
            if (isFinishing() || isDestroyed()) return;
            Log.d(LOG_TAG, "No ads: " + reason);
            hideAds();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (InitiativeTrackerApp) getApplication();
        encounter = app.getEncounter();
        isTwoPane = findViewById(R.id.second_fragment_container) != null;

        setUpDrawer();
        applySystemBarInsets();

        setUpPanes(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);
        onBackStackChanged();
        showActiveParty();

        adBar = findViewById(R.id.bottomAdBar);
        if (!app.isAdsRemoved()) {
            adConsentManager = new AdConsentManager(this);
            adConsentManager.requestConsentThenInit(adCallback);
        }

        billingManager = new BillingManager(getApplication());
        billingManager.setRemoveAdsListener(new BillingManager.RemoveAdsListener() {
            @Override
            public void onAdsRemoved() {
                hideAds();
            }

            @Override
            public void onPriceLoaded() {
                refreshDrawer();
            }
        });
        billingManager.setPartyPackListener((level, fresh) -> {
            if (fresh) {
                Toast.makeText(this, getString(R.string.billing_party_pack_thanks,
                        app.getParties().getLimit()), Toast.LENGTH_LONG).show();
            }
            refreshDrawer();
        });
        billingManager.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adBar.resume();
    }

    @Override
    protected void onPause() {
        app.saveEncounter();
        adBar.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        billingManager.endConnection();
        adBar.destroyCurrentAd();
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
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // ---------------------------------------------------------------- Drawer: parties

    @Override
    public void onSwitchParty(String id) {
        if (id.equals(app.getParties().getActiveId())) return;
        app.switchParty(id);
        onActivePartyChanged();
    }

    @Override
    public void onNewParty() {
        PartyBook parties = app.getParties();
        if (!parties.canCreate()) {
            showPartyLimit();
            return;
        }
        String suggestion = PartyNames.newName(getString(R.string.party_new_name_prefix), parties.getParties());
        PartyDialogs.askName(this, R.string.dialog_party_new_title, R.string.dialog_party_create,
                suggestion, parties, null, name -> {
                    Party party = app.createParty(name);
                    if (party != null) onSwitchParty(party.getId());
                });
    }

    @Override
    public void onRenameParty(String id) {
        Party party = app.getParties().get(id);
        if (party == null) return;
        PartyDialogs.askName(this, R.string.dialog_party_rename_title, R.string.dialog_party_save,
                party.getName(), app.getParties(), id, name -> {
                    app.getParties().rename(id, name);
                    showActiveParty();
                    refreshDrawer();
                });
    }

    @Override
    public void onDuplicateParty(String id) {
        if (!app.getParties().canCreate()) {
            showPartyLimit();
            return;
        }
        if (app.duplicateParty(id) != null) refreshDrawer();
    }

    @Override
    public void onDeleteParty(String id) {
        Party party = app.getParties().get(id);
        if (party == null) return;
        if (app.getParties().getUsed() <= 1) {
            Toast.makeText(this, R.string.toast_party_last, Toast.LENGTH_SHORT).show();
            return;
        }
        PartyDialogs.confirmDelete(this, party.getName(), app.characterCount(id), () -> {
            boolean wasActive = id.equals(app.getParties().getActiveId());
            if (!app.deleteParty(id)) return;
            if (wasActive) {
                onActivePartyChanged();
            } else {
                refreshDrawer();
            }
        });
    }

    @Override
    public void onGetMoreParties() {
        PartyBook parties = app.getParties();
        int level = PartySlots.nextPackLevel(parties.getPackLevel());
        String price = level > 0 ? billingManager.getPartyPackPrice(level) : null;
        if (price == null) {
            Toast.makeText(this, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        PartyDialogs.offerPack(this, parties.getUsed(), parties.getLimit(), price,
                () -> billingManager.purchasePartyPack(this, level));
    }

    // ---------------------------------------------------------------- Drawer: app items

    @Override
    public void onRemoveAds() {
        billingManager.purchaseRemoveAds(this);
    }

    @Override
    public void onTip() {
        billingManager.openTipDialog(this);
    }

    @Override
    public void onAbout() {
        InfoDialogs.about(this);
    }

    @Override
    public void onPrivacy() {
        InfoDialogs.privacyPolicy(this);
    }

    @Override
    public void onPrivacySettings() {
        if (adConsentManager != null) adConsentManager.showPrivacyOptionsForm(adCallback);
    }

    // ---------------------------------------------------------------- Panes

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

    /**
     * Another party is in the fight now: back to the list (the open details or Edit Order belong to
     * the old party), the details of its first character in landscape, and the new name on top.
     */
    private void onActivePartyChanged() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (isTwoPane) {
            Fragment detailPane = fm.findFragmentById(R.id.second_fragment_container);
            if (encounter.size() > 0) {
                showCharacterDetail(0);
            } else if (detailPane != null) {
                fm.beginTransaction().remove(detailPane).commit();
            }
        }
        showActiveParty();
        refreshDrawer();
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

    // ---------------------------------------------------------------- Drawer, toolbar, ads

    private void setUpDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navDrawer = new NavDrawer(navView.getHeaderView(0), this, drawerLayout::closeDrawers);

        // Back closes the drawer first
        OnBackPressedCallback closeDrawerOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawers();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDrawerOnBack);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                // Fresh counts and round before it slides in, also when swiped open
                if (newState != DrawerLayout.STATE_IDLE && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    refreshDrawer();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawerOnBack.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                closeDrawerOnBack.setEnabled(false);
            }
        });
        refreshDrawer();
    }

    /** Fills the drawer: parties, slots, the next pack (only when it exists in Play), app items. */
    private void refreshDrawer() {
        if (navDrawer == null) return;
        boolean adsRemoved = app.isAdsRemoved();
        String removeAdsPrice = null;
        if (!adsRemoved) {
            String price = billingManager != null ? billingManager.getRemoveAdsPrice() : null;
            removeAdsPrice = price != null ? price : "";
        }
        // Only when Google's consent SDK says users must be able to change their choice (usually EEA/UK)
        boolean showPrivacySettings = !adsRemoved
                && adConsentManager != null && adConsentManager.isPrivacyOptionsRequired();
        int nextLevel = PartySlots.nextPackLevel(app.getParties().getPackLevel());
        String nextPackPrice = nextLevel > 0 && billingManager != null
                ? billingManager.getPartyPackPrice(nextLevel) : null;
        navDrawer.refresh(app, new NavDrawer.State(removeAdsPrice, showPrivacySettings, nextPackPrice));
    }

    /** All slots used: offers the next pack when it exists in Play, else says to delete one. */
    private void showPartyLimit() {
        PartyBook parties = app.getParties();
        int level = PartySlots.nextPackLevel(parties.getPackLevel());
        String price = level > 0 ? billingManager.getPartyPackPrice(level) : null;
        PartyDialogs.showLimit(this, parties.getUsed(), parties.getLimit(), price,
                () -> billingManager.purchasePartyPack(this, level));
    }

    /** The active party's name under the app name in the toolbar. */
    private void showActiveParty() {
        toolbar.setSubtitle(app.getParties().getActive().getName());
    }

    /** No ads (consent failed, or Remove Ads bought): the bar goes away and the drawer updates. */
    private void hideAds() {
        adBar.destroyCurrentAd();
        adBar.setVisibility(View.GONE);
        refreshDrawer();
    }

    /** The menu icon opens the drawer on the list; on details or Edit Order it is a back arrow. */
    private void onBackStackChanged() {
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (canGoBack) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationContentDescription(R.string.nav_back);
            toolbar.setNavigationOnClickListener(v -> getSupportFragmentManager().popBackStack());
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationContentDescription(R.string.nav_drawer_open);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        // The drawer only swipes open on the list, so a swipe on the details doesn't open it
        drawerLayout.setDrawerLockMode(canGoBack
                ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
        invalidateOptionsMenu();
    }

    /**
     * Edge-to-edge: pads the screen for the status bar, navigation bar and display cutout, and
     * for the keyboard when it is open. The drawer gets the same insets: its header below the
     * status bar, its bottom above the navigation bar. Done here for both, so it works the same
     * on every Android version.
     */
    private void applySystemBarInsets() {
        View root = findViewById(R.id.root);
        NavigationView navView = findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0);
        int headerTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            navView.setPadding(bars.left, 0, 0, bars.bottom);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
