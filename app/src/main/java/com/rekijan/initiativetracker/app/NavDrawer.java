package com.rekijan.initiativetracker.app;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.rekijan.initiativetracker.R;

/**
 * Fills the navigation drawer (layout nav_header) and passes taps to the {@link Listener}: the
 * open profile with Switch profile; for the GM the encounter actions (Add character, Delete
 * characters, Edit order) and Table sync (with the hosted code); then Remove Ads, Tip the dev,
 * About, Privacy Policy and Privacy Settings. A player and the profile list only get the app
 * items. Call {@link #refresh} after anything it shows has changed.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class NavDrawer {

    /** What the drawer rows do; implemented by MainActivity. */
    public interface Listener {
        void onSwitchProfile();

        void onAddCharacter();

        void onDeleteCharacters();

        void onEditOrder();

        void onTableSync();

        void onRemoveAds();

        void onTip();

        void onAbout();

        void onPrivacy();

        void onPrivacySettings();
    }

    /** Which screen the drawer belongs to. */
    public enum Mode {
        /** A party's encounter: all items. */
        GM,
        /** A player character: no encounter or Table sync items. */
        PLAYER,
        /** The profile list: no profile is open. */
        PROFILES
    }

    /** What the drawer shows; decided by MainActivity. */
    public static final class State {
        final Mode mode;
        /** The header line: party and round, or the character, or null on the profile list. */
        @Nullable
        final String profileLine;
        /** Null hides Remove Ads; empty shows it without a price. */
        @Nullable
        final String removeAdsPrice;
        final boolean showPrivacySettings;

        public State(Mode mode, @Nullable String profileLine, @Nullable String removeAdsPrice,
                     boolean showPrivacySettings) {
            this.mode = mode;
            this.profileLine = profileLine;
            this.removeAdsPrice = removeAdsPrice;
            this.showPrivacySettings = showPrivacySettings;
        }
    }

    private final View header;
    private final Runnable closeDrawer;

    public NavDrawer(View header, Listener listener, Runnable closeDrawer) {
        this.header = header;
        this.closeDrawer = closeDrawer;

        bind(R.id.nav_switch_profile, listener::onSwitchProfile);
        bind(R.id.nav_add_character, listener::onAddCharacter);
        bind(R.id.nav_delete_characters, listener::onDeleteCharacters);
        bind(R.id.nav_edit_order, listener::onEditOrder);
        bind(R.id.nav_table_sync, listener::onTableSync);
        bind(R.id.nav_remove_ads, listener::onRemoveAds);
        bind(R.id.nav_tip, listener::onTip);
        bind(R.id.nav_about, listener::onAbout);
        bind(R.id.nav_privacy, listener::onPrivacy);
        bind(R.id.nav_privacy_settings, listener::onPrivacySettings);
    }

    public void refresh(InitiativeTrackerApp app, State state) {
        Context context = header.getContext();
        boolean gm = state.mode == Mode.GM;

        TextView profileText = header.findViewById(R.id.nav_profile_textView);
        profileText.setText(state.profileLine != null ? state.profileLine : context.getString(R.string.nav_no_profile));
        header.findViewById(R.id.nav_switch_profile)
                .setVisibility(state.mode == Mode.PROFILES ? View.GONE : View.VISIBLE);
        header.findViewById(R.id.nav_group_encounter).setVisibility(gm ? View.VISIBLE : View.GONE);

        TextView tableSync = header.findViewById(R.id.nav_table_sync);
        String hostCode = app.getSync().getHostCode();
        tableSync.setText(hostCode != null ? context.getString(R.string.nav_table_sync_hosting, hostCode)
                : context.getString(R.string.nav_table_sync));

        TextView removeAds = header.findViewById(R.id.nav_remove_ads);
        removeAds.setVisibility(state.removeAdsPrice != null ? View.VISIBLE : View.GONE);
        if (state.removeAdsPrice != null) {
            removeAds.setText(state.removeAdsPrice.isEmpty()
                    ? context.getString(R.string.action_remove_ads)
                    : context.getString(R.string.action_remove_ads_price, state.removeAdsPrice));
        }
        header.findViewById(R.id.nav_privacy_settings)
                .setVisibility(state.showPrivacySettings ? View.VISIBLE : View.GONE);
    }

    private void bind(int viewId, Runnable action) {
        header.findViewById(viewId).setOnClickListener(v -> {
            closeDrawer.run();
            action.run();
        });
    }
}
