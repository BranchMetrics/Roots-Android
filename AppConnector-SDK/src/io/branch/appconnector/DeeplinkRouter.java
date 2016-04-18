package io.branch.appconnector;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by sojanpr on 4/12/16.
 * <p>
 * Class for handling the deep link routing up on application launch by App connector SDK.
 * The deep link activity is figured out by matching the "al:android:url" metadata for the activity.
 * The matching activity is launched automatically with the parameter values specified in the deep link path
 * </p>
 */
class DeeplinkRouter {

    private int activityCnt_;
    private static final String ANDROID_AL_URL_KEY = "al:android:url";
    private static final String WEB_AL_URL_KEY = "al:web:url";


    public DeeplinkRouter() {
        activityCnt_ = 0;
    }

    public void enable(Application application) {
        Application.ActivityLifecycleCallbacks lifecycleCallbacks_ = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (activityCnt_ == 0 && isActivityLaunchedByAppConnector(activity)) {
                    Intent intent = getAnyDeepLinkMatchIntent(activity);
                    if (intent != null) {
                        activity.startActivity(intent);
                    }
                }
                activityCnt_++;
            }

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {
                activityCnt_--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        };
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks_);
    }

    /**
     * Checks if the activity is launched by App Connector SDK from some other application.
     *
     * @param activity Activity launched
     * @return True if this activity is launched by App Connector SDK from some other application.
     */
    public static boolean isActivityLaunchedByAppConnector(Activity activity) {
        boolean isAppConnectorLaunch = false;
        if (activity != null && activity.getIntent() != null && activity.getIntent().getData() != null) {
            Uri data = activity.getIntent().getData();
            if (data.getQueryParameter(Defines.APP_CONNECTOR_LAUNCH_KEY) != null) {
                isAppConnectorLaunch = Boolean.parseBoolean(data.getQueryParameter(Defines.APP_CONNECTOR_LAUNCH_KEY));
            }
        }
        return isAppConnectorLaunch;
    }

    /**
     * Checks if the a activity is launched by App connector SDK deep link routing
     *
     * @param activity Activity launched
     * @return True if this activity is launched by App Connector SDK on finding a deep link path.
     */
    public static boolean isActivityLaunchedByDeepLinkRouter(Activity activity) {
        boolean isAppConnectorDeeplinkLaunch = false;
        if (activity != null && activity.getIntent() != null) {
            isAppConnectorDeeplinkLaunch = activity.getIntent().getBooleanExtra(Defines.APP_CONNECTOR_DEEPLINK_LAUNCH_KEY, false);
        }
        return isAppConnectorDeeplinkLaunch;
    }

    private Intent getAnyDeepLinkMatchIntent(Activity activity) {
        Intent intent = null;
        if (activity != null && activity.getIntent() != null && activity.getIntent().getData() != null) {
            Uri launchedUri = activity.getIntent().getData();
            String launcherUriStr = launchedUri.toString().toLowerCase();
            String fallbackUri = launchedUri.getQueryParameter(Defines.APP_CONNECTOR_FALLBACK_URL);
            HashMap<String, String> fallBackUrlActivityMap_ = new HashMap<>();
            try {
                PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                ActivityInfo[] activityInfos = info.activities;

                if (activityInfos != null) {
                    boolean strongMatchFound = false;
                    for (ActivityInfo activityInfo : activityInfos) {
                        if (activityInfo != null && activityInfo.metaData != null) {
                            // Check if any activity is filtering the fallback url. This used to do a weak matching in case strong URI match not found
                            if (!TextUtils.isEmpty(fallbackUri)) {
                                String fallbackUrlPattern = activityInfo.metaData.getString(WEB_AL_URL_KEY);
                                if (!TextUtils.isEmpty(fallbackUrlPattern)) {
                                    if (Matcher.matchUriPattern(fallbackUri, fallbackUrlPattern)) {
                                        fallBackUrlActivityMap_.put(activityInfo.name, fallbackUrlPattern);
                                    }
                                }
                            }
                            // Check for a  potential URI path for any strong match
                            if (activityInfo.metaData.getString(ANDROID_AL_URL_KEY) != null) {
                                String deeplinkUriPattern = activityInfo.metaData.getString(ANDROID_AL_URL_KEY);
                                if (Matcher.matchUriPattern(launcherUriStr, deeplinkUriPattern)) {
                                    strongMatchFound = true;
                                    intent = Matcher.createTargetIntent(activity, activityInfo.name, launcherUriStr, deeplinkUriPattern);
                                    break;
                                }
                            }
                        }
                    }
                    if (!strongMatchFound) { // If there  is not strong URI match check for a fallback URL match
                        if (fallBackUrlActivityMap_.size() > 0) {
                            String targetActivityName = fallBackUrlActivityMap_.keySet().iterator().next();
                            String fallbackUriPattern = fallBackUrlActivityMap_.get(targetActivityName);
                            intent = Matcher.createTargetIntent(activity, targetActivityName, fallbackUri, fallbackUriPattern);
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException | ClassNotFoundException ignore) {
            }
        }
        return intent;
    }



}
