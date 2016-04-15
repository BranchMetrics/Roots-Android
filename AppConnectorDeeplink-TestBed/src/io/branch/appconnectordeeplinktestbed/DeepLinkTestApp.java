package io.branch.appconnectordeeplinktestbed;

import android.app.Application;

import io.branch.appconnector.AppConnector;

/**
 * <p>
 *  Application class for Applink deeplink testbed
 * <p/>
 *
 */
public class DeepLinkTestApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppConnector.enableDeeplinkRouting(this);
    }
}
