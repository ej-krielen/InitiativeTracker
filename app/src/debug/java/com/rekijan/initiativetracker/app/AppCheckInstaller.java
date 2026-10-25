package com.rekijan.initiativetracker.app;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/**
 * Sets up Firebase App Check for debug builds with the debug provider, so the emulator and test
 * phones work once their debug token (printed in Logcat) is registered in the Firebase console.
 * The release version uses Play Integrity.
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
                DebugAppCheckProviderFactory.getInstance());
    }
}
