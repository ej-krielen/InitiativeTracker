package com.rekijan.initiativetracker.app;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/**
 * Sets up Firebase App Check for release builds with Play Integrity, so only the real app from
 * Google Play can reach Firestore once enforcement is on. The debug version uses the debug provider.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class AppCheckInstaller {

    private AppCheckInstaller() {
    }

    public static void install(Context context) {
        FirebaseApp.initializeApp(context);
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
