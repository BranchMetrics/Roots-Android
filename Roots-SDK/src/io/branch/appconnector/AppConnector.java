package io.branch.appconnector;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;

/**
 * Created by sojanpr on 4/5/16.
 * <p>
 * Main class for app to app connection. Class provide a simple builder pattern to connect to other apps
 * or getting app metadata using urls and other custom attributes. Class also provides option to customise standard app link structure
 * to handle custom metadata and referrer information.
 * </p>
 */
public class AppConnector {
    /* URL set for opening the app */
    private final String url_;
    private final Activity activity_;
    private boolean alwaysFallbackToPlayStore_;
    private IAppConnectionEvents connectionEventsCallback_;
    private String browserAgentString_ = null;
    private boolean isUserOverridingFallbackRule_;

    /**
     * <p>
     * Create an instance of {@link AppConnector} to open the app
     * </p>
     *
     * @param url {@link String} with value for the URL to open
     */
    public AppConnector(@NonNull Activity activity, @NonNull String url) {
        activity_ = activity;
        url_ = url.toLowerCase(); // Weired Android issue HTTP wont work
        alwaysFallbackToPlayStore_ = false;
    }


    //-----------------Launcher side functionalities---------------------------------------------------//

    /**
     * <p>
     * Setting this option will try to open the play store  when there is no application installed.
     * The web url will be opened only if unable to open play store
     * </p>
     *
     * @param alwaysFallbackToPlayStore true to enable always fallback to Playstore
     * @return {@link AppConnector} instance for method chaining
     */
    @SuppressWarnings("unused")
    public AppConnector setAlwaysFallbackToPlayStore(boolean alwaysFallbackToPlayStore) {
        isUserOverridingFallbackRule_ = true;
        alwaysFallbackToPlayStore_ = alwaysFallbackToPlayStore;
        return this;
    }

    /**
     * Sets an instance of {@link io.branch.appconnector.AppConnector.IAppConnectionEvents } to get called back with
     * App connect events
     *
     * @param appConnectionEvents {@link io.branch.appconnector.AppConnector.IAppConnectionEvents} instance
     * @return {@link AppConnector} instance for method chaining
     */
    @SuppressWarnings("unused")
    public AppConnector setAppConnectionEventsCallback(IAppConnectionEvents appConnectionEvents) {
        connectionEventsCallback_ = appConnectionEvents;
        return this;
    }

    /**
     * Sets an optional browser string to access and inspect the navigation url.
     *
     * @param browserAgentString {@link String} a fully qualified browser agent string
     * @return {@link AppConnector} instance for method chaining
     */
    @SuppressWarnings("unused")
    public AppConnector setBrowserAgent(String browserAgentString) {
        browserAgentString_ = browserAgentString;
        return this;
    }


    /**
     * <p>
     * Open the app if there is a matching app installed for the given url. Opens a fallback url if app is not installed.
     * </p>
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void connect() {
        // 1. Try to open the app without scraping the app link tags in case of app links
        if (AppRouter.resolveUrlToAppWithoutPackageName(activity_, url_, connectionEventsCallback_)) {
            // Launched with app linked to url
        }
        // 2. If no app with matching app linked to the url scrape the Url for app link meta data
        else {
            AppConnectionExtractor.scrapeAppLinkTags(activity_, url_, browserAgentString_, new AppConnExtractionEvents());
        }
    }

    /**
     * <p>
     * Method to debug app connector with debug app link data.
     * </p>
     *
     * @param url                  {@link String} URL to open
     * @param applinkDebugMetadata {@link JSONArray} with debug app link metadata
     */
    public void debugConnect(String url, JSONArray applinkDebugMetadata) {
        AppLaunchConfig appLaunchConfig = new AppLaunchConfig(applinkDebugMetadata, url);
        AppRouter.handleAppRouting(activity_, appLaunchConfig, connectionEventsCallback_);
    }

    private class AppConnExtractionEvents implements AppConnectionExtractor.IAppConnectionExtractorEvents {
        @Override
        public void onAppLaunchConfigAvailable(final AppLaunchConfig appLaunchConfig, AppConnectionExtractor.CONN_EXTRACT_ERR err) {
            if (err == AppConnectionExtractor.CONN_EXTRACT_ERR.NO_ERROR) {
                if (isUserOverridingFallbackRule_) {
                    appLaunchConfig.setAlwaysOpenPlayStore(alwaysFallbackToPlayStore_);
                }
                activity_.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppRouter.handleAppRouting(activity_, appLaunchConfig, connectionEventsCallback_);
                    }
                });

            } else {
                if (appLaunchConfig != null && !TextUtils.isEmpty(appLaunchConfig.getTargetAppFallbackUrl())) {

                    activity_.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AppRouter.openFallbackUrl(activity_, appLaunchConfig, connectionEventsCallback_);
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                }
            }
        }
    }


    public interface IAppConnectionEvents {
        /**
         * <p>
         * Called when the connecting app is launched successfully
         * </p>
         *
         * @param appName     {@link String} with value connection app name
         * @param packageName {@link String} with value connection app package name
         */
        void onAppLaunched(String appName, String packageName);

        /**
         * <p>
         * Called up on opening a fallback url when app is not installed on the device
         * </p>
         *
         * @param url {@link String} with value for the fallback url
         */
        void onFallbackUrlOpened(String url);

        /**
         * <p>
         * Called  when user is navigated to the play store to download the app since the connecting app is not installed.
         * </p>
         *
         * @param appName     {@link String} with value connection app name
         * @param packageName {@link String} with value connection app package name
         */
        void onPlayStoreOpened(String appName, String packageName);
    }


    //---------------------- Receiver side functionalities-----------------------------------------------//

    /**
     * Check if the activity is launched by AppConnector SDK
     *
     * @param activity Activity to check if it is launched by app connector
     * @return A {@link Boolean} whose value is set to true if the activity specified is launched by AppConnector SDK
     */
    @SuppressWarnings("unused")
    public static boolean isAppConnectorLaunched(Activity activity) {
        return (DeeplinkRouter.isActivityLaunchedByDeepLinkRouter(activity)
                || DeeplinkRouter.isActivityLaunchedByAppConnector(activity));

    }

    /**
     * Enables in app routing based on the app link filter added to activities. Should be called from application create event
     *
     * @param application {@link Application} object
     */
    public static void enableDeeplinkRouting(Application application) {
        new DeeplinkRouter().enable(application);
    }


}
