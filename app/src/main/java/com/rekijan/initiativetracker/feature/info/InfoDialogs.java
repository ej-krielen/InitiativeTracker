package com.rekijan.initiativetracker.feature.info;

import android.content.Context;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;

/**
 * The info dialogs from the menu: About (links to the website, and to Licenses), Licenses (the
 * icons and the bundled fonts), Privacy Policy (short summary with a link to the full policy) and
 * how to delete a character.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class InfoDialogs {

    private static final String WEBSITE_URL = "https://rekijan.nl/";
    private static final String PRIVACY_URL = "https://rekijan.nl/privacypolicy.php";

    private InfoDialogs() {
    }

    public static void about(Context context) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_about_info_title)
                .setMessage(R.string.dialog_about_info)
                .setPositiveButton(R.string.dialog_about_ok, (d, w) -> Dialogs.openUrl(context, WEBSITE_URL))
                .setNeutralButton(R.string.licenses_button, (d, w) -> licenses(context))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * Shows the licenses of the icons and the bundled fonts (Manrope, Cinzel). The SIL Open Font
     * License asks for the font license text to ship with the app.
     */
    public static void licenses(Context context) {
        Dialogs.builder(context)
                .setTitle(R.string.licenses_title)
                .setMessage(Dialogs.readRawText(context, R.raw.licenses))
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    public static void privacyPolicy(Context context) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_privacy_info_title)
                .setMessage(R.string.dialog_privacy_info)
                .setPositiveButton(R.string.dialog_privacy_ok, (d, w) -> Dialogs.openUrl(context, PRIVACY_URL))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    public static void howToDeleteCharacter(Context context) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_delete_info_title)
                .setMessage(R.string.dialog_delete_info)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }
}
