package com.rekijan.initiativetracker.core.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.rekijan.initiativetracker.R;

/**
 * One place where the app's dialogs get their style, so the restyle only has to change this.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static AlertDialog.Builder builder(Context context) {
        return new AlertDialog.Builder(context, R.style.AlertDialogStyle);
    }

    /** Opens a web page, or tells the user when there is no browser. */
    public static void openUrl(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, url, Toast.LENGTH_LONG).show();
        }
    }
}
