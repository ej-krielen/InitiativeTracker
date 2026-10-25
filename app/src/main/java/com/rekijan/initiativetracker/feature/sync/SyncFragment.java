package com.rekijan.initiativetracker.feature.sync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.party.Party;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Table sync for the GM, opened from the drawer: start a session for the active party, show the
 * code to read out or share, the players who sent a character, and Stop session. Players join
 * from their own character screen. All the work is done by {@link SyncManager}; this screen only
 * shows its state.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class SyncFragment extends Fragment implements SyncManager.StateListener {

    private InitiativeTrackerApp app;
    private SyncManager sync;
    private boolean busy;

    public static SyncFragment newInstance() {
        return new SyncFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (InitiativeTrackerApp) requireActivity().getApplication();
        sync = app.getSync();
        view.findViewById(R.id.sync_start_button).setOnClickListener(v -> startHosting());
        view.findViewById(R.id.sync_stop_button).setOnClickListener(v -> confirmStop());
        view.findViewById(R.id.sync_copy_button).setOnClickListener(v -> copyCode());
        view.findViewById(R.id.sync_share_button).setOnClickListener(v -> shareCode());
        sync.addStateListener(this);
        render();
    }

    @Override
    public void onDestroyView() {
        sync.removeStateListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSyncStateChanged() {
        if (getView() != null) render();
    }

    private void render() {
        View view = requireView();
        String code = sync.getHostCode();
        boolean hosting = code != null;
        TextView intro = view.findViewById(R.id.sync_host_intro_textView);
        intro.setVisibility(hosting ? View.GONE : View.VISIBLE);
        intro.setText(getString(R.string.sync_host_intro, app.getParties().getActive().getName()));
        view.findViewById(R.id.sync_start_button).setVisibility(hosting ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.sync_code_card).setVisibility(hosting ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sync_players_label_textView).setVisibility(hosting ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sync_stop_button).setVisibility(hosting ? View.VISIBLE : View.GONE);
        LinearLayout list = view.findViewById(R.id.sync_players_list);
        list.removeAllViews();
        if (!hosting) return;

        ((TextView) view.findViewById(R.id.sync_code_textView)).setText(code);
        Party party = app.getParties().get(sync.getHostPartyId());
        String partyName = party != null ? party.getName() : "";
        String partyText = getString(R.string.sync_host_party, partyName);
        if (party != null && !party.getId().equals(app.getParties().getActiveId())) {
            partyText += "\n" + getString(R.string.sync_host_other_party, partyName);
        }
        ((TextView) view.findViewById(R.id.sync_host_party_textView)).setText(partyText);

        Map<String, PlayerSheet> players = sync.getPlayers();
        TextView label = view.findViewById(R.id.sync_players_label_textView);
        label.setText(players.isEmpty() ? getString(R.string.sync_players_none)
                : getString(R.string.sync_players_count, players.size()));
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (PlayerSheet sheet : players.values()) {
            View row = inflater.inflate(R.layout.item_sync_player, list, false);
            ((TextView) row.findViewById(R.id.sync_player_name_textView)).setText(sheet.name);
            ((TextView) row.findViewById(R.id.sync_player_stats_textView)).setText(
                    getString(R.string.sync_player_stats, sheet.initiative, sheet.hp, sheet.maxHp));
            list.addView(row);
        }
    }

    private void startHosting() {
        if (busy) return;
        setBusy(true);
        sync.startHosting(new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                setBusy(false);
            }

            @Override
            public void onNotFound() {
                setBusy(false);
            }

            @Override
            public void onFailure(Exception e) {
                setBusy(false);
                toast(errorMessage(e));
            }
        });
    }

    private void confirmStop() {
        Dialogs.builder(requireContext())
                .setTitle(R.string.sync_stop_title)
                .setMessage(R.string.sync_stop_message)
                .setPositiveButton(R.string.sync_stop, (d, w) -> sync.stopHosting(
                        new SyncRepository.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                // The screen already shows Start session again
                            }

                            @Override
                            public void onFailure(Exception e) {
                                toast(R.string.sync_stop_offline);
                            }
                        }))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void copyCode() {
        String code = sync.getHostCode();
        if (code == null) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.sync_code_label), code));
        toast(R.string.sync_copied);
    }

    private void shareCode() {
        String code = sync.getHostCode();
        if (code == null) return;
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.sync_share_text, code));
        startActivity(Intent.createChooser(send, getString(R.string.sync_share)));
    }

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        View view = getView();
        if (view != null) view.findViewById(R.id.sync_start_button).setEnabled(!isBusy);
    }

    /** @return A message for a failed Table sync action, shared with the player screen. */
    @StringRes
    public static int errorMessage(Exception e) {
        if (e instanceof TimeoutException) return R.string.sync_error_offline;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return R.string.sync_error_offline;
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return R.string.sync_error_denied;
        }
        return R.string.sync_error_generic;
    }

    private void toast(@StringRes int message) {
        Context context = getContext();
        if (context != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
