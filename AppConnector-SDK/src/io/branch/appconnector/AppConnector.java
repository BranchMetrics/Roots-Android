package io.branch.appconnector;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;

import java.security.PrivateKey;

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
    private final Context context_;
    private boolean alwaysFallbackToWebUrl_;
    private IAppConnectionEvents connectionEventsCallback_;

    /**
     * <p>
     * Create an instance of {@link AppConnector} to open the app
     * </p>
     *
     * @param url {@link String} with value for the URL to open
     */
    public AppConnector(Context context, String url) {
        context_ = context;
        url_ = url;
        alwaysFallbackToWebUrl_ = false;
    }


    //-----------------Launcher side functionalities---------------------------------------------------//

    /**
     * <p>
     *     Setting this option will open the app link web url always when there is no application installed.
     *     Otherwise the default behaviour is try to open the play store when there is no application installed.
     * </p>
     * @param alwaysFallbackToWebUrl true to enable always fallback to web url
     * @return {@link AppConnector} instance for method chaining
     */
    public AppConnector setAlwaysFallbackToWebUrl(boolean alwaysFallbackToWebUrl) {
        alwaysFallbackToWebUrl_ = alwaysFallbackToWebUrl;
        return this;
    }

    /**
     * Sets an instance of {@link io.branch.appconnector.AppConnector.IAppConnectionEvents } to get called back with
     * App connect events
     * @param appConnectionEvents {@link io.branch.appconnector.AppConnector.IAppConnectionEvents} instance
     * @return {@link AppConnector} instance for method chaining
     */
    public AppConnector setAppConnectionEventsCallback(IAppConnectionEvents appConnectionEvents) {
        connectionEventsCallback_ = appConnectionEvents;
        return this;
    }



    /**
     * <p>
     * Open the app if there is a matching app installed for the given url. Opens a fallback url if app is not installed.
     * </p>
     *
     * @return {@link Boolean} with value true if app is opened else false.
     */
    public boolean connect() {
        boolean isAppOpened = false;
        AppConnectionExtractor.scrapeAppLinkTags(context_, url_, new AppConnExtractionEvents());
        return isAppOpened;
    }

    public boolean debugConnect(String url, JSONArray applinkDebugMetadata) {
        boolean isAppOpened = false;
        AppLaunchConfig appLaunchConfig = new AppLaunchConfig(applinkDebugMetadata, url);
        AppRouter.handleAppRouting(context_, appLaunchConfig, connectionEventsCallback_);
        return isAppOpened;
    }

    private class AppConnExtractionEvents implements AppConnectionExtractor.IAppConnectionExtractorEvents {
        @Override
        public void onAppLaunchConfigAvailable(AppLaunchConfig appLaunchConfig, AppConnectionExtractor.CONN_EXTRACT_ERR err) {
            if (err == AppConnectionExtractor.CONN_EXTRACT_ERR.NO_ERROR) {
                appLaunchConfig.setAlwaysOpenWebUrl(alwaysFallbackToWebUrl_);
                AppRouter.handleAppRouting(context_, appLaunchConfig, connectionEventsCallback_);
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
     * @param activity Activity to check if it is launched by app connector
     * @return A {@link Boolean} whose value is set to true if the activity specified is launched by AppConnector SDK
     */
    public static boolean isAppConnectorLaunched(Activity activity){
       return  (DeeplinkRouter.isActivityLaunchedByDeepLinkRouter(activity)
               || DeeplinkRouter.isActivityLaunchedByAppConnector(activity));

    }

    /**
     * Enables in app routing based on the app link filter added to activities. Should be called from application create event
     * @param application {@link Application} object
     */
    public static void enableDeeplinkRouting(Application application){
        new DeeplinkRouter().enable(application);
    }





}
