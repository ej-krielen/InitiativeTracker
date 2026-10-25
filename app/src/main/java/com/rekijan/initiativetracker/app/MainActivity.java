package com.rekijan.initiativetracker.app;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.rekijan.initiativetracker.BuildConfig;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.feature.character.CharacterDefaults;
import com.rekijan.initiativetracker.feature.character.CharacterDetailFragment;
import com.rekijan.initiativetracker.feature.encounter.CharacterDialogs;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.encounter.EncounterFragment;
import com.rekijan.initiativetracker.feature.encounter.TurnDialogs;
import com.rekijan.initiativetracker.feature.info.InfoDialogs;
import com.rekijan.initiativetracker.feature.order.EditOrderFragment;
import com.rekijan.initiativetracker.feature.party.PartyBook;
import com.rekijan.initiativetracker.feature.party.PartyDialogs;
import com.rekijan.initiativetracker.feature.party.PartySlots;
import com.rekijan.initiativetracker.feature.player.PlayerFragment;
import com.rekijan.initiativetracker.feature.profile.ProfilesFragment;
import com.rekijan.initiativetracker.feature.sync.ApprovalDialogs;
import com.rekijan.initiativetracker.feature.sync.SyncDiff;
import com.rekijan.initiativetracker.feature.sync.SyncFragment;
import com.rekijan.initiativetracker.feature.sync.SyncManager;
import com.rekijan.initiativetracker.monetization.BillingManager;

import java.util.List;

import nl.rekijan.ads.AdConsentManager;
import nl.rekijan.ads.RekijanBannerAdView;

/**
 * The only activity. Opens the profile that was used last: a party's encounter (the GM) or a
 * player character ({@link PlayerFragment}). Back from there, or Switch profile in the drawer,
 * goes to the profile list ({@link ProfilesFragment}); back on the list closes the app.
 * <p>
 * For the GM, the encounter list is swapped in portrait for the character details, Edit order or
 * Table sync; in landscape (layout-land) the details sit next to the list. The toolbar holds Next
 * turn and Sort; the navigation drawer ({@link NavDrawer}) holds Add character, Delete
 * characters, Edit order and Table sync, and the app items. It also runs the ad consent flow and
 * the ad bar, billing (tips, Remove Ads, party packs), shows the GM's Table sync requests
 * ({@link ApprovalDialogs}) one at a time, and saves the fight when the app goes to the background.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class MainActivity extends AppCompatActivity implements NavDrawer.Listener, ProfilesFragment.Host {

    private static final String LOG_TAG = "MainActivity";

    private InitiativeTrackerApp app;
    private Encounter encounter;
    private BillingManager billingManager;
    private RekijanBannerAdView adBar;
    private AdConsentManager adConsentManager;
    private MaterialToolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavDrawer navDrawer;
    private OnBackPressedCallback profileRootBack;
    private boolean isTwoPane;
    @Nullable
    private AlertDialog requestDialog;

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

        // Back callbacks: the one added last wins, so the drawer (close first) comes after this one
        setUpProfileRootBack();
        setUpDrawer();
        applySystemBarInsets();

        setUpPanes(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(this::onScreenChanged);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                              @NonNull View v, @Nullable Bundle savedInstanceState) {
                // Root screens are swapped without the back stack, so follow the views instead
                if (f.getId() == R.id.main_fragment_container) onScreenChanged();
            }
        }, false);

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
            Fragment main = mainPane();
            if (main instanceof ProfilesFragment) ((ProfilesFragment) main).refresh();
        });
        billingManager.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adBar.resume();
        app.getSync().setRequestListener(this::showNextRequest);
        showNextRequest();
    }

    @Override
    protected void onPause() {
        app.getSync().setRequestListener(null);
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
        Fragment main = mainPane();
        boolean onEncounter = main instanceof EncounterFragment || main instanceof CharacterDetailFragment;
        menu.setGroupVisible(R.id.group_encounter, onEncounter);
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
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // ---------------------------------------------------------------- Profiles

    @Override
    public void openParty(String id) {
        app.openParty(id);
        showRoot(EncounterFragment.newInstance());
    }

    @Override
    public void openPlayer(String id) {
        app.openPlayer(id);
        showRoot(PlayerFragment.newInstance(id));
    }

    @Override
    public void showPartyLimit() {
        PartyBook parties = app.getParties();
        int level = PartySlots.nextPackLevel(parties.getPackLevel());
        String price = level > 0 ? billingManager.getPartyPackPrice(level) : null;
        PartyDialogs.showLimit(this, parties.getUsed(), parties.getLimit(), price,
                () -> billingManager.purchasePartyPack(this, level));
    }

    @Override
    public void offerMoreParties() {
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

    @Nullable
    @Override
    public String nextPackPrice() {
        int level = PartySlots.nextPackLevel(app.getParties().getPackLevel());
        return level > 0 && billingManager != null ? billingManager.getPartyPackPrice(level) : null;
    }

    // ---------------------------------------------------------------- Drawer

    @Override
    public void onSwitchProfile() {
        showRoot(ProfilesFragment.newInstance());
    }

    @Override
    public void onAddCharacter() {
        closeDetailsInOnePane();
        encounter.add(CharacterDefaults.newCharacter(this));
    }

    @Override
    public void onDeleteCharacters() {
        closeDetailsInOnePane();
        CharacterDialogs.askDeleteCharacters(this, encounter, this::showFirstCharacterInTwoPanes);
    }

    @Override
    public void onEditOrder() {
        if (mainPane() instanceof EditOrderFragment) return;
        closeDetailsInOnePane();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, EditOrderFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onTableSync() {
        if (mainPane() instanceof SyncFragment) return;
        closeDetailsInOnePane();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, SyncFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

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

    /**
     * First start: the profile used last. After rotating, the screens are restored; only the
     * details move between one and two panes.
     */
    private void setUpPanes(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            String playerId = app.getOpenPlayerId();
            fm.beginTransaction()
                    .add(R.id.main_fragment_container, playerId != null
                            ? PlayerFragment.newInstance(playerId) : EncounterFragment.newInstance())
                    .commit();
            if (playerId == null) showFirstCharacterInTwoPanes();
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
        } else if (detailPane == null && isGmScreen(mainPane)) {
            showFirstCharacterInTwoPanes();
        }
    }

    /**
     * Puts a root screen (encounter, player or profile list) in the main pane: the back stack and
     * the details pane are cleared, and in landscape the encounter gets its first character's details.
     */
    private void showRoot(Fragment root) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fm.beginTransaction().replace(R.id.main_fragment_container, root).commitNow();
        Fragment detailPane = fm.findFragmentById(R.id.second_fragment_container);
        if (detailPane != null) fm.beginTransaction().remove(detailPane).commitNow();
        if (root instanceof EncounterFragment) showFirstCharacterInTwoPanes();
        onScreenChanged();
    }

    /** In landscape: the details of the first character, or nothing when the party is empty. */
    private void showFirstCharacterInTwoPanes() {
        if (!isTwoPane) return;
        if (encounter.size() > 0) {
            showCharacterDetail(0);
        } else {
            Fragment detailPane = getSupportFragmentManager().findFragmentById(R.id.second_fragment_container);
            if (detailPane != null) getSupportFragmentManager().beginTransaction().remove(detailPane).commit();
        }
    }

    private void nextTurn() {
        Encounter.Turn turn = encounter.nextTurn();
        if (turn == null) return;
        TurnDialogs.onTurnStarted(this, encounter, turn);
        TurnDialogs.showTicks(findViewById(R.id.main_fragment_container), adBar, turn);
        // In two panes the details follow whoever's turn it is
        if (isTwoPane) showCharacterDetail(0);
    }

    /** Encounter actions go back to the list first when there is one pane. */
    private void closeDetailsInOnePane() {
        FragmentManager fm = getSupportFragmentManager();
        if (!isTwoPane && fm.getBackStackEntryCount() > 0) fm.popBackStackImmediate();
    }

    @Nullable
    private Fragment mainPane() {
        return getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
    }

    /** @return True for the GM's screens: the encounter and what opens from it. */
    private static boolean isGmScreen(@Nullable Fragment f) {
        return f != null && !(f instanceof PlayerFragment) && !(f instanceof ProfilesFragment);
    }

    // ---------------------------------------------------------------- Drawer, toolbar, back, ads

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
                // Fresh round and code before it slides in, also when swiped open
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
    }

    /** Back on a profile's main screen (encounter or character) goes to the profile list. */
    private void setUpProfileRootBack() {
        profileRootBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showRoot(ProfilesFragment.newInstance());
            }
        };
        getOnBackPressedDispatcher().addCallback(this, profileRootBack);
    }

    /** Fills the drawer for the screen that is open. */
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
        Fragment main = mainPane();
        NavDrawer.Mode mode;
        String line;
        if (main instanceof ProfilesFragment) {
            mode = NavDrawer.Mode.PROFILES;
            line = null;
        } else if (main instanceof PlayerFragment) {
            mode = NavDrawer.Mode.PLAYER;
            line = getString(R.string.nav_player_line, playerName(((PlayerFragment) main).getCharacterId()));
        } else {
            mode = NavDrawer.Mode.GM;
            line = getString(R.string.nav_active_party, app.getParties().getActive().getName(), encounter.getRound());
        }
        navDrawer.refresh(app, new NavDrawer.State(mode, line, removeAdsPrice, showPrivacySettings));
    }

    private String playerName(String id) {
        String name = app.getPlayers().getName(id);
        return name.isEmpty() ? getString(R.string.empty_character_name) : name;
    }

    /** No ads (consent failed, or Remove Ads bought): the bar goes away and the drawer updates. */
    private void hideAds() {
        adBar.destroyCurrentAd();
        adBar.setVisibility(View.GONE);
        refreshDrawer();
    }

    /**
     * Another screen is open: the toolbar subtitle, the menu or back arrow, which back goes to the
     * profile list, the second pane (only for the GM) and the drawer follow.
     */
    private void onScreenChanged() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment main = mainPane();
        boolean canGoBack = fm.getBackStackEntryCount() > 0;

        if (main instanceof ProfilesFragment) {
            toolbar.setSubtitle(null);
        } else if (main instanceof PlayerFragment) {
            toolbar.setSubtitle(getString(R.string.nav_player_line, playerName(((PlayerFragment) main).getCharacterId())));
        } else {
            toolbar.setSubtitle(app.getParties().getActive().getName());
        }

        if (canGoBack) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationContentDescription(R.string.nav_back);
            toolbar.setNavigationOnClickListener(v -> fm.popBackStack());
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationContentDescription(R.string.nav_drawer_open);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        // The drawer only swipes open on a root screen, so a swipe on the details doesn't open it
        drawerLayout.setDrawerLockMode(canGoBack
                ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
        profileRootBack.setEnabled(!canGoBack && !(main instanceof ProfilesFragment));

        if (isTwoPane) {
            int visibility = isGmScreen(main) ? View.VISIBLE : View.GONE;
            findViewById(R.id.second_fragment_container).setVisibility(visibility);
            findViewById(R.id.pane_divider).setVisibility(visibility);
        }
        invalidateOptionsMenu();
        refreshDrawer();
        showNextRequest();
    }

    /**
     * Shows the next Table sync request for the GM, one at a time and only on the GM's screens
     * while this screen is in front. After an answer the next one in the queue follows.
     */
    private void showNextRequest() {
        if (requestDialog != null && requestDialog.isShowing()) return;
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED) || !isGmScreen(mainPane())) return;
        SyncManager sync = app.getSync();
        SyncManager.Request request = sync.nextRequest();
        if (request == null) return;
        ApprovalDialogs.Listener answer = new ApprovalDialogs.Listener() {
            @Override
            public void onApprove(List<SyncDiff.Change> accepted) {
                sync.approve(request, accepted);
            }

            @Override
            public void onRefuse() {
                sync.refuse(request);
            }
        };
        String name = request.sheet.name.isEmpty() ? getString(R.string.empty_character_name) : request.sheet.name;
        requestDialog = request.join
                ? ApprovalDialogs.showJoin(this, request.sheet, app.getParties().getActive().getName(), answer)
                : ApprovalDialogs.showChanges(this, name, request.changes, answer);
        requestDialog.setOnDismissListener(d -> {
            requestDialog = null;
            showNextRequest();
        });
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
