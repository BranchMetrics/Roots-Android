package io.branch.rootstestbed;

import android.app.Application;

import io.branch.roots.Roots;

/**
 * <p>
 * Application class for Roots Testbed.
 * </p>
 */
public class TestBedApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Deep link routing to auto deep link to configured activities
        Roots.enableDeeplinkRouting(this);
    }
}
