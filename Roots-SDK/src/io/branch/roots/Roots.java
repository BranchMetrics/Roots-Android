package io.branch.roots;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sojanpr on 4/5/16.
 * <p>
 * Main class for app to app connection. Class provide a simple builder pattern to connect to other apps
 * or getting app metadata using urls and other custom attributes. Class also provides option to customise standard app link structure
 * to handle custom metadata and referrer information.
 * </p>
 */
public class Roots {
    /* URL set for opening the app */
    private final String url_;
    private final Activity activity_;
    private boolean alwaysFallbackToPlayStore_;
    private IRootsEvents connectionEventsCallback_;
    private String browserAgentString_ = null;
    private boolean isUserOverridingFallbackRule_;
    private final Map<String, String> additionalLinkData_;
    private boolean registerLinkClickIfAppIsNotInstalled_;
    
    /**
     * <p>
     * Create an instance of {@link Roots} to open the app
     * </p>
     *
     * @param url {@link String} with value for the URL to open
     */
    public Roots(@NonNull Activity activity, @NonNull String url) {
        activity_ = activity;
        // Weired Android issue HTTP wont work
        url = url.replace("HTTPS://", "https://");
        url = url.replace("HTTP://", "http://");
        url_ = url;
        additionalLinkData_ = new HashMap<>();
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
     * @return {@link Roots} instance for method chaining
     */
    @SuppressWarnings("unused")
    public Roots setAlwaysFallbackToPlayStore(boolean alwaysFallbackToPlayStore) {
        isUserOverridingFallbackRule_ = true;
        alwaysFallbackToPlayStore_ = alwaysFallbackToPlayStore;
        return this;
    }
    
    /**
     * Sets an instance of {@link Roots.IRootsEvents } to get called back with
     * App connect events
     *
     * @param rootConnectionEvents {@link Roots.IRootsEvents} instance
     * @return {@link Roots} instance for method chaining
     */
    @SuppressWarnings("unused")
    public Roots setRootsConnectionEventsCallback(IRootsEvents rootConnectionEvents) {
        connectionEventsCallback_ = rootConnectionEvents;
        return this;
    }
    
    /**
     * Sets an optional browser string to access and inspect the navigation url.
     *
     * @param browserAgentString {@link String} a fully qualified browser agent string
     * @return {@link Roots} instance for method chaining
     */
    @SuppressWarnings("unused")
    public Roots setBrowserAgent(String browserAgentString) {
        browserAgentString_ = browserAgentString;
        return this;
    }
    
    /***
     * Adds any additional data that need to be added to the link. These data will be added as query params.
     * In case of Branch deep link / deferred deep link these params are provided as link data.
     * @param key {@link String} Key for the additional link data
     * @param value {@link String} value for additional link data
     * @return {@link Roots} instance for method chaining
     */
    public Roots addAdditionalLinkData(String key, String value) {
        additionalLinkData_.put(key, value);
        return this;
    }
    
    /**
     * If called Roots will simulate a link open in the browser. Call this method in case  need to register a click especially if you need to get deferred link params while app is installed.
     * Should be used  when deferred deep linking is preferred with out opening browser or play store app etc.
     * Note: This will not open browser with fallback url
     *
     * @return {@link Roots} instance for method chaining
     */
    public Roots registerLinkClickIfAppIsNotInstalled() {
        registerLinkClickIfAppIsNotInstalled_ = true;
        return this;
    }
    
    /**
     * <p>
     * Open the app if there is a matching app installed for the given url. Opens a fallback url or Simulate a link open in browser in case of app not installed depending on the settings.
     * see {@link #registerLinkClickIfAppIsNotInstalled()} {@link #setAlwaysFallbackToPlayStore()}
     * </p>
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void connect() {
        String modifiedUrl = getUrlWithAdditionalData(url_);
        // 1. Try to open the app without scraping the app link tags in case of app links
        if (AppRouter.resolveUrlToAppWithoutPackageName(activity_, modifiedUrl, connectionEventsCallback_)) {
            // Launched with app linked to url
        } else if (registerLinkClickIfAppIsNotInstalled_) {
            // 2. Register a click on actula link if opted
            RootsFinder.registerClick(activity_, modifiedUrl, browserAgentString_, connectionEventsCallback_);
        } else {
            // 3. If no app with matching app linked to the url scrape the Url for app link meta data
            RootsFinder.scrapeAppLinkTags(activity_, modifiedUrl, browserAgentString_, new RootsFinderEvents());
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
        url = getUrlWithAdditionalData(url);
        AppLaunchConfig appLaunchConfig = new AppLaunchConfig(applinkDebugMetadata, url);
        AppRouter.handleAppRouting(activity_, appLaunchConfig, connectionEventsCallback_);
    }
    
    private String getUrlWithAdditionalData(String url) {
        String modifiedUrl = url;
        try {
            Uri.Builder builtUri = Uri.parse(url_).buildUpon();
            for (String key : additionalLinkData_.keySet()) {
                builtUri.appendQueryParameter(key, additionalLinkData_.get(key));
            }
            modifiedUrl = builtUri.build().toString();
        } catch (Exception ignore) {
        }
        return modifiedUrl;
    }
    
    private class RootsFinderEvents implements RootsFinder.IRootsConnectionExtractorEvents {
        @Override
        public void onAppLaunchConfigAvailable(final AppLaunchConfig appLaunchConfig, RootsFinder.CONN_EXTRACT_ERR err) {
            if (err == RootsFinder.CONN_EXTRACT_ERR.NO_ERROR) {
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
    
    
    public interface IRootsEvents {
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
     * Check if the activity is launched by Roots SDK
     *
     * @param activity Activity to check if it is launched by app connector
     * @return A {@link Boolean} whose value is set to true if the activity specified is launched by Roots SDK
     */
    @SuppressWarnings("unused")
    public static boolean isRootsLaunched(Activity activity) {
        return (DeeplinkRouter.isActivityLaunchedByDeepLinkRouter(activity)
                || DeeplinkRouter.isActivityLaunchedByRoots(activity));
        
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
