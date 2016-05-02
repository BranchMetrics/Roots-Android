package io.branch.rootsroutingtestbed;

import android.app.Application;

import io.branch.roots.Roots;

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
        Roots.enableDeeplinkRouting(this);
    }
}
