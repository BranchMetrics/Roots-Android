package io.branch.roots;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sojanpr on 4/6/16.
 * <p>
 * Class for defining the target application configuration
 * </p>
 */
class AppLaunchConfig {

    private static final String PROPERTY_KEY = "property";
    private static final String CONTENT_KEY = "content";

    private static final String PROPERTY_ANDROID_URL = "al:android:url";
    private static final String PROPERTY_ANDROID_APP_NAME = "al:android:app_name";
    private static final String PROPERTY_ANDROID_PACKAGE_NAME = "al:android:package";
    private static final String PROPERTY_WEB_URL = "al:web:url";
    private static final String PROPERTY_ALWAYS_WEB_FALLBACK = "al:web:should_fallback";

    public static final int PORT_UNDEFINED = -1;

    private String actualUri_;
    private String targetUri_;
    private String targetAppName_;
    private String targetAppLaunchScheme_;
    private String targetAppLaunchHost_;
    private String targetAppLaunchPath_;
    private int targetAppLaunchPort_ = PORT_UNDEFINED;
    private String targetAppLaunchParams_;
    private String targetAppPackageName_;
    private String targetAppFallbackUrl_;
    private boolean alwaysOpenPlayStore_ = true;


    /**
     * <p>
     * Create an instance of {@link AppLaunchConfig} with the given app link metadata
     * </p>
     *
     * @param appLinkMetaDataArray {@link JSONArray} representing the app link metadata
     * @param url                  {@link String} with value for the target url
     */
    public AppLaunchConfig(JSONArray appLinkMetaDataArray, String url) {
        createFromAppLinkData(appLinkMetaDataArray, url);
    }

    /**
     * Get the actual uri for opening the app
     *
     * @return {@link String} with value of actual url used for app connection.
     */
    public String getActualUri() {
        return actualUri_;
    }

    /**
     * Get the actual target uri for opening the app
     *
     * @return {@link String} with value of target uri used for app connection.
     */
    public String getTargetUri() {
        return targetUri_;
    }

    /**
     * Get the package name for the target application
     *
     * @return {@link String} with value for target application name
     */
    public String getTargetAppName() {
        return targetAppName_;
    }

    /**
     * Get the scheme uri to launch the target app
     *
     * @return {@link String} with value for the target uri scheme
     */
    public String getTargetAppLaunchScheme() {
        return targetAppLaunchScheme_;
    }

    /**
     * Get the host for the uri to launch the target app
     *
     * @return {@link String} with value for the target app uri host
     */
    public String getTargetAppLaunchHost() {
        return targetAppLaunchHost_;
    }

    /**
     * get the path for the uri to launch the target app
     *
     * @return  {@link String} with value for the target app uri path
     */
    public String getTargetAppLaunchPath() {
        return targetAppLaunchPath_;
    }

    /**
     * Get any port for teh uri to launch the target app
     *
     * @return {@link Integer} with value for the port. Return PORT_UNDEFINED if there is no valid port
     */
    public int getTargetAppLaunchPort() {
        return targetAppLaunchPort_;
    }

    /**
     * Get the query params for the uri to open the app
     *
     * @return {@link String} with value for the target app uri query params
     */
    public String getTargetAppLaunchParams() {
        return targetAppLaunchParams_;
    }

    /**
     * Get the package name for the target app
     *
     * @return {@link String} with value target app package name
     */
    public String getTargetAppPackageName() {
        return targetAppPackageName_;
    }

    /**
     * Get the url to fallback in case of app not installed
     *
     * @return {@link String} with value fall back url
     */
    public String getTargetAppFallbackUrl() {
        return targetAppFallbackUrl_;
    }

    /**
     * Specifies if the fall back is set to web url always
     *
     * @return {@link Boolean} with value true if set to open the web url always
     */
    @SuppressWarnings("unused")
    public boolean isAlwaysOpenPlayStore() {
        return alwaysOpenPlayStore_;
    }

    /**
     * Specifies if the fall back is set to PlayStore  always
     *
     * @param alwaysOpenPlayStore True to set always fallback to playStore
     */
    public void setAlwaysOpenPlayStore(boolean alwaysOpenPlayStore) {
        this.alwaysOpenPlayStore_ = alwaysOpenPlayStore;
    }

    /**
     * Create an {@link AppLaunchConfig} instance  form App Link meta data
     *
     * @param metadataArray App Link metadata array
     * @param url           {@link String} with value for the target url
     */
    private void createFromAppLinkData(JSONArray metadataArray, String url) {
        actualUri_ = url;
        actualUri_ = actualUri_.toLowerCase();
        // Default value for the fallback is the url itself
        targetAppFallbackUrl_ = url;
        if (metadataArray != null) {
            try {
                for (int i = 0; i < metadataArray.length(); i++) {
                    JSONObject metaDataJson = metadataArray.getJSONObject(i);
                    if (metaDataJson.has(PROPERTY_KEY) && metaDataJson.has(CONTENT_KEY)) {
                        String property = metaDataJson.getString(PROPERTY_KEY);
                        String value = metaDataJson.getString(CONTENT_KEY);

                        if (property.equalsIgnoreCase(PROPERTY_ANDROID_APP_NAME)) {
                            targetAppName_ = value;
                        } else if (property.equalsIgnoreCase(PROPERTY_ANDROID_PACKAGE_NAME)) {
                            targetAppPackageName_ = value;
                        } else if (property.equalsIgnoreCase(PROPERTY_WEB_URL)) {
                            targetAppFallbackUrl_ = value;
                        } else if (property.equalsIgnoreCase(PROPERTY_ALWAYS_WEB_FALLBACK)) {
                            try {
                                alwaysOpenPlayStore_ = Boolean.parseBoolean(value);
                            } catch (Exception ignore) {

                            }
                        } else if (property.equalsIgnoreCase(PROPERTY_ANDROID_URL)) {
                            Uri uri = Uri.parse(value);
                            targetUri_ = value;
                            targetAppLaunchScheme_ = uri.getScheme();
                            targetAppLaunchHost_ = uri.getHost();
                            targetAppLaunchPath_ = uri.getPath();
                            targetAppLaunchParams_ = uri.getQuery();
                            targetAppLaunchPort_ = uri.getPort();
                        }
                    }
                }
            } catch (JSONException ignore) {

            }
        }
    }


    public boolean isLaunchIntentAvailable() {
        return (!TextUtils.isEmpty(targetAppLaunchScheme_) && !TextUtils.isEmpty(targetAppPackageName_));
    }
}
