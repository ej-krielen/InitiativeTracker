package com.rekijan.initiativetracker.core.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RawRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * One place where the app's dialogs are built: MaterialAlertDialogBuilder, so they follow the
 * theme (Cinzel title, gold buttons, see ThemeOverlay.Rekijan.Dialog).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class Dialogs {

    private static final String LOG_TAG = "Dialogs";

    private Dialogs() {
    }

    public static MaterialAlertDialogBuilder builder(Context context) {
        return new MaterialAlertDialogBuilder(context);
    }

    /** Opens a web page, or tells the user when there is no browser. */
    public static void openUrl(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, url, Toast.LENGTH_LONG).show();
        }
    }

    /** Reads a text file from res/raw. Returns an empty string if it cannot be read. */
    public static String readRawText(Context context, @RawRes int rawResId) {
        StringBuilder text = new StringBuilder();
        try (InputStream in = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not read raw resource " + rawResId, e);
        }
        return text.toString();
    }
}
