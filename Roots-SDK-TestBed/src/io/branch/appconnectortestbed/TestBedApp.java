package io.branch.appconnectortestbed;

import android.app.Application;

import io.branch.appconnector.AppConnector;

/**
 * <p>
 * Application class for AppConnector Testbed.
 * </p>
 */
public class TestBedApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Deep link routing to auto deep link to configured activities
        AppConnector.enableDeeplinkRouting(this);
    }
}