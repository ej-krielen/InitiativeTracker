package com.rekijan.initiativetracker.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.feature.party.Party;
import com.rekijan.initiativetracker.feature.party.PartyBook;

/**
 * Fills the navigation drawer (layout nav_header) and passes taps to the {@link Listener}: the
 * party list (tap to switch, the three dots for Rename, Duplicate, Delete), New party, the used
 * slots and Get more parties, Table sync (with the hosted or joined code), then Remove Ads, Tip the dev, About, Privacy Policy and Privacy
 * Settings. Call {@link #refresh} after anything it shows has changed.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class NavDrawer {

    /** What the drawer rows do; implemented by MainActivity. */
    public interface Listener {
        void onSwitchParty(String id);

        void onNewParty();

        void onRenameParty(String id);

        void onDuplicateParty(String id);

        void onDeleteParty(String id);

        void onGetMoreParties();

        void onTableSync();

        void onRemoveAds();

        void onTip();

        void onAbout();

        void onPrivacy();

        void onPrivacySettings();
    }

    /** What the drawer shows besides the parties; decided by MainActivity. */
    public static final class State {
        /** Null hides Remove Ads; empty shows it without a price. */
        @Nullable
        final String removeAdsPrice;
        final boolean showPrivacySettings;
        /** Price of the next party pack, or null to hide Get more parties. */
        @Nullable
        final String nextPackPrice;

        public State(@Nullable String removeAdsPrice, boolean showPrivacySettings, @Nullable String nextPackPrice) {
            this.removeAdsPrice = removeAdsPrice;
            this.showPrivacySettings = showPrivacySettings;
            this.nextPackPrice = nextPackPrice;
        }
    }

    private final View header;
    private final Listener listener;
    private final Runnable closeDrawer;
    private final LinearLayout partyList;

    public NavDrawer(View header, Listener listener, Runnable closeDrawer) {
        this.header = header;
        this.listener = listener;
        this.closeDrawer = closeDrawer;
        partyList = header.findViewById(R.id.nav_party_list);

        bind(R.id.nav_new_party, listener::onNewParty);
        bind(R.id.nav_get_more_parties, listener::onGetMoreParties);
        bind(R.id.nav_table_sync, listener::onTableSync);
        bind(R.id.nav_remove_ads, listener::onRemoveAds);
        bind(R.id.nav_tip, listener::onTip);
        bind(R.id.nav_about, listener::onAbout);
        bind(R.id.nav_privacy, listener::onPrivacy);
        bind(R.id.nav_privacy_settings, listener::onPrivacySettings);
    }

    public void refresh(InitiativeTrackerApp app, State state) {
        Context context = header.getContext();
        PartyBook parties = app.getParties();
        Party active = parties.getActive();

        TextView activeText = header.findViewById(R.id.nav_active_party_textView);
        activeText.setText(context.getString(R.string.nav_active_party,
                active.getName(), app.getEncounter().getRound()));

        partyList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (Party party : parties.getParties()) {
            partyList.addView(partyRow(inflater, app, party, party == active, parties.getUsed() > 1));
        }

        TextView slots = header.findViewById(R.id.nav_party_slots_textView);
        slots.setText(context.getString(R.string.nav_party_slots, parties.getUsed(), parties.getLimit()));
        header.findViewById(R.id.nav_get_more_parties)
                .setVisibility(state.nextPackPrice != null ? View.VISIBLE : View.GONE);

        TextView tableSync = header.findViewById(R.id.nav_table_sync);
        String hostCode = app.getSync().getHostCode();
        String playerCode = app.getSync().getPlayerCode();
        tableSync.setText(hostCode != null ? context.getString(R.string.nav_table_sync_hosting, hostCode)
                : playerCode != null ? context.getString(R.string.nav_table_sync_joined, playerCode)
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

    private View partyRow(LayoutInflater inflater, InitiativeTrackerApp app, Party party,
                          boolean isActive, boolean canDelete) {
        View row = inflater.inflate(R.layout.item_nav_party, partyList, false);
        TextView name = row.findViewById(R.id.party_name_textView);
        name.setText(party.getName());
        name.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        ((TextView) row.findViewById(R.id.party_count_textView))
                .setText(String.valueOf(app.characterCount(party.getId())));
        row.setSelected(isActive);
        row.setOnClickListener(v -> {
            closeDrawer.run();
            listener.onSwitchParty(party.getId());
        });

        View more = row.findViewById(R.id.party_more_button);
        more.setContentDescription(row.getContext().getString(R.string.nav_party_options, party.getName()));
        more.setOnClickListener(v -> showPartyMenu(v, party.getId(), canDelete));
        return row;
    }

    private void showPartyMenu(View anchor, String partyId, boolean canDelete) {
        Context themed = new ContextThemeWrapper(anchor.getContext(), R.style.ThemeOverlay_Rekijan_Menu);
        PopupMenu menu = new PopupMenu(themed, anchor);
        menu.getMenu().add(0, R.id.party_menu_rename, 0, R.string.party_menu_rename);
        menu.getMenu().add(0, R.id.party_menu_duplicate, 1, R.string.party_menu_duplicate);
        // The last party can't be deleted, so it gets no Delete
        if (canDelete) menu.getMenu().add(0, R.id.party_menu_delete, 2, R.string.party_menu_delete);
        menu.setOnMenuItemClickListener(item -> {
            closeDrawer.run();
            int id = item.getItemId();
            if (id == R.id.party_menu_rename) {
                listener.onRenameParty(partyId);
            } else if (id == R.id.party_menu_duplicate) {
                listener.onDuplicateParty(partyId);
            } else {
                listener.onDeleteParty(partyId);
            }
            return true;
        });
        menu.show();
    }

    private void bind(int viewId, Runnable action) {
        header.findViewById(viewId).setOnClickListener(v -> {
            closeDrawer.run();
            action.run();
        });
    }
}
