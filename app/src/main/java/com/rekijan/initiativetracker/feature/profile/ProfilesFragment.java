package com.rekijan.initiativetracker.feature.profile;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.party.Party;
import com.rekijan.initiativetracker.feature.party.PartyBook;
import com.rekijan.initiativetracker.feature.party.PartyDialogs;
import com.rekijan.initiativetracker.feature.party.PartyNames;
import com.rekijan.initiativetracker.feature.player.PlayerBook;
import com.rekijan.initiativetracker.feature.sync.SyncManager;

import java.util.Collections;

/**
 * The profile list, like Secret Roller's start screen: the user's parties (GM) and their player
 * characters (Player) in one list. Tap a row to open it; the three dots hold Rename, Duplicate
 * (parties only) and Delete. New makes a party (within the party slots) or a character (free,
 * no limit). Opening and buying are passed to the {@link Host}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class ProfilesFragment extends Fragment implements SyncManager.StateListener {

    /** What the list needs from the activity; implemented by MainActivity. */
    public interface Host {
        void openParty(String id);

        void openPlayer(String id);

        /** All party slots are used: offers the next pack, or says to delete one. */
        void showPartyLimit();

        /** "Get more parties": offers the next pack. */
        void offerMoreParties();

        /** @return The price of the next party pack, or null when there is none to buy. */
        @Nullable
        String nextPackPrice();
    }

    private InitiativeTrackerApp app;

    public static ProfilesFragment newInstance() {
        return new ProfilesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (InitiativeTrackerApp) requireActivity().getApplication();
        view.findViewById(R.id.profiles_new_button).setOnClickListener(v -> askNewKind());
        view.findViewById(R.id.profiles_more_parties_button).setOnClickListener(v -> host().offerMoreParties());
        app.getSync().addStateListener(this);
        refresh();
    }

    @Override
    public void onDestroyView() {
        app.getSync().removeStateListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSyncStateChanged() {
        if (getView() != null) refresh();
    }

    /** Redraws the list; also called by MainActivity after a purchase. */
    public void refresh() {
        View view = getView();
        if (view == null) return;
        PartyBook parties = app.getParties();
        PlayerBook players = app.getPlayers();
        SyncManager sync = app.getSync();
        boolean partyOpen = !app.isPlayerOpen();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        ((TextView) view.findViewById(R.id.profiles_parties_label_textView)).setText(
                getString(R.string.profiles_parties_label, parties.getUsed(), parties.getLimit()));
        LinearLayout partyList = view.findViewById(R.id.profiles_party_list);
        partyList.removeAllViews();
        for (Party party : parties.getParties()) {
            boolean hosting = party.getId().equals(sync.getHostPartyId()) && sync.getHostCode() != null;
            int count = app.characterCount(party.getId());
            String detail = getResources().getQuantityString(R.plurals.profiles_party_detail, count, count);
            if (hosting) detail = getString(R.string.profiles_hosting, detail, sync.getHostCode());
            boolean open = partyOpen && party.getId().equals(parties.getActiveId());
            partyList.addView(row(inflater, partyList, party.getName(), detail, hosting, true, open,
                    v -> host().openParty(party.getId()), v -> showPartyMenu(v, party)));
        }
        view.findViewById(R.id.profiles_more_parties_button)
                .setVisibility(host().nextPackPrice() != null ? View.VISIBLE : View.GONE);

        LinearLayout playerList = view.findViewById(R.id.profiles_player_list);
        playerList.removeAllViews();
        for (String id : players.getIds()) {
            String code = sync.getPlayerCode(id);
            String detail = code != null ? getString(R.string.sync_joined, code) : getString(R.string.profiles_not_joined);
            String name = players.getName(id);
            if (name.isEmpty()) name = getString(R.string.empty_character_name);
            boolean open = id.equals(app.getOpenPlayerId());
            String finalName = name;
            playerList.addView(row(inflater, playerList, name, detail, code != null, false, open,
                    v -> host().openPlayer(id), v -> showPlayerMenu(v, id, finalName)));
        }
        view.findViewById(R.id.profiles_players_empty_textView)
                .setVisibility(players.getIds().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View row(LayoutInflater inflater, ViewGroup parent, String name, String detail, boolean lamp,
                     boolean gm, boolean open, View.OnClickListener onOpen, View.OnClickListener onMore) {
        View row = inflater.inflate(R.layout.item_profile, parent, false);
        ((TextView) row.findViewById(R.id.profile_name_textView)).setText(name);
        ((TextView) row.findViewById(R.id.profile_detail_textView)).setText(detail);
        row.findViewById(R.id.profile_status_lamp).setVisibility(lamp ? View.VISIBLE : View.GONE);
        TextView role = row.findViewById(R.id.profile_role_textView);
        role.setText(gm ? R.string.profiles_role_gm : R.string.profiles_role_player);
        role.setTextColor(ContextCompat.getColor(requireContext(), gm ? R.color.rk_npc_marker : R.color.rk_pc_marker));
        role.setBackgroundColor(ContextCompat.getColor(requireContext(), gm ? R.color.rk_npc_container : R.color.rk_pc_container));
        MaterialCardView card = (MaterialCardView) row;
        if (open) {
            // The open profile gets a gold outline
            card.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.profile_open_stroke));
            card.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.rk_pc_marker)));
        }
        row.setOnClickListener(onOpen);
        View more = row.findViewById(R.id.profile_more_button);
        more.setContentDescription(getString(R.string.nav_party_options, name));
        more.setOnClickListener(onMore);
        return row;
    }

    // ---------------------------------------------------------------- New

    private void askNewKind() {
        String[] kinds = {getString(R.string.profiles_new_party), getString(R.string.profiles_new_character)};
        Dialogs.builder(requireContext())
                .setTitle(R.string.profiles_new_title)
                .setItems(kinds, (d, which) -> {
                    if (which == 0) {
                        newParty();
                    } else {
                        newCharacter();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void newParty() {
        PartyBook parties = app.getParties();
        if (!parties.canCreate()) {
            host().showPartyLimit();
            return;
        }
        String suggestion = PartyNames.newName(getString(R.string.party_new_name_prefix), parties.getParties());
        PartyDialogs.askName(requireContext(), R.string.dialog_party_new_title, R.string.dialog_party_create,
                suggestion, parties, null, name -> {
                    Party party = app.createParty(name);
                    if (party != null) host().openParty(party.getId());
                });
    }

    private void newCharacter() {
        PartyDialogs.askName(requireContext(), R.string.profiles_new_character_title, R.string.dialog_party_create,
                R.string.profiles_character_name_hint, "",
                name -> PartyNames.check(name, Collections.emptyList(), null), name -> {
                    String id = app.getPlayers().create(name);
                    if (id != null) host().openPlayer(id);
                });
    }

    // ---------------------------------------------------------------- Options

    private void showPartyMenu(View anchor, Party party) {
        PopupMenu menu = popup(anchor);
        menu.getMenu().add(0, 1, 0, R.string.party_menu_rename);
        menu.getMenu().add(0, 2, 1, R.string.party_menu_duplicate);
        menu.getMenu().add(0, 3, 2, R.string.party_menu_delete);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    PartyDialogs.askName(requireContext(), R.string.dialog_party_rename_title, R.string.dialog_party_save,
                            party.getName(), app.getParties(), party.getId(), name -> {
                                app.getParties().rename(party.getId(), name);
                                refresh();
                            });
                    return true;
                case 2:
                    if (!app.getParties().canCreate()) {
                        host().showPartyLimit();
                    } else if (app.duplicateParty(party.getId()) != null) {
                        refresh();
                    }
                    return true;
                case 3:
                    deleteParty(party);
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void deleteParty(Party party) {
        if (app.getParties().getUsed() <= 1) {
            Toast.makeText(requireContext(), R.string.toast_party_last, Toast.LENGTH_SHORT).show();
            return;
        }
        PartyDialogs.confirmDelete(requireContext(), party.getName(), app.characterCount(party.getId()), () -> {
            app.deleteParty(party.getId());
            refresh();
        });
    }

    private void showPlayerMenu(View anchor, String id, String name) {
        PopupMenu menu = popup(anchor);
        menu.getMenu().add(0, 1, 0, R.string.party_menu_rename);
        menu.getMenu().add(0, 3, 1, R.string.party_menu_delete);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                PartyDialogs.askName(requireContext(), R.string.profiles_rename_character_title, R.string.dialog_party_save,
                        R.string.profiles_character_name_hint, app.getPlayers().getName(id),
                        n -> PartyNames.check(n, Collections.emptyList(), null), n -> {
                            app.getPlayers().rename(id, n);
                            refresh();
                        });
                return true;
            }
            if (item.getItemId() == 3) {
                Dialogs.builder(requireContext())
                        .setTitle(R.string.profiles_delete_character_title)
                        .setMessage(getString(R.string.profiles_delete_character_message, name))
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.dialog_party_delete_button, (d, w) -> {
                            app.deletePlayer(id);
                            refresh();
                        })
                        .show();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private PopupMenu popup(View anchor) {
        return new PopupMenu(new ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_Rekijan_Menu), anchor);
    }

    private Host host() {
        return (Host) requireActivity();
    }
}
