package io.branch.roots;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sojanpr on 4/7/16.
 * <p>
 * Class for handling the routing with the given {@link AppLaunchConfig}.
 * Try to open a matching app if installed otherwise fallback to play store or web url.
 * </p>
 */
class AppRouter {

    public static boolean handleAppRouting(Context context, AppLaunchConfig appLaunchConfig, Roots.IRootsEvents callback) {
        boolean routingHandled = true;
        try {
            if (appLaunchConfig.isLaunchIntentAvailable()) {
                if (isAppInstalled(context, appLaunchConfig.getTargetAppPackageName())) {  //Open the app if app is installed
                    // 1. Check if the actual url is a configured app link
                    if (launchOnAppLinkMatchingForUrl(context, appLaunchConfig.getActualUri(), appLaunchConfig, callback)) {
                        // Launched app with App link association for actual uri

                    }
                    // 2. Check if the target android uri is configured for app link
                    else if (launchOnAppLinkMatchingForUrl(context, appLaunchConfig.getTargetUri(), appLaunchConfig, callback)) {
                        // Launched app with App link association for target uri
                    }
                    // 3. Check id the android uri is a non app linked uri
                    else {
                        openAppWithUriScheme(context, appLaunchConfig, callback);
                    }

                } else { // If app is not installed
                    handleAppNotInstalled(context, appLaunchConfig, callback);
                }
            } else {
                openFallbackUrl(context, appLaunchConfig, callback);

            }
        } catch (UnsupportedEncodingException ex) {
            routingHandled = false;
        }
        return routingHandled;
    }


    ////---------------------------- Private methods---------------------------------------------------------//


    private static void openAppWithUriScheme(Context context, AppLaunchConfig appLaunchConfig, Roots.IRootsEvents callback) throws UnsupportedEncodingException {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setPackage(appLaunchConfig.getTargetAppPackageName());
        String uriString = appLaunchConfig.getTargetAppLaunchScheme() + "://";
        if (!TextUtils.isEmpty(appLaunchConfig.getTargetAppLaunchHost())) {
            uriString += appLaunchConfig.getTargetAppLaunchHost();
        }
        if (appLaunchConfig.getTargetAppLaunchPort() != AppLaunchConfig.PORT_UNDEFINED) {
            uriString += ":" + appLaunchConfig.getTargetAppLaunchPort();
        }
        if (!TextUtils.isEmpty(appLaunchConfig.getTargetAppLaunchPath())) {
            uriString += appLaunchConfig.getTargetAppLaunchPath();
        }

        uriString += "?" + Defines.APP_CONNECTOR_LAUNCH_KEY + "=True";
        uriString += "&" + Defines.APP_CONNECTOR_FALLBACK_URL + "=" + appLaunchConfig.getTargetAppFallbackUrl();

        if (!TextUtils.isEmpty(appLaunchConfig.getTargetAppLaunchParams())) {
            uriString += "&" + appLaunchConfig.getTargetAppLaunchParams();
        }
        Uri uri = Uri.parse(uriString);
        intent.setData(uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            if (callback != null) {
                callback.onAppLaunched(appLaunchConfig.getTargetAppName(), appLaunchConfig.getTargetAppPackageName());
            }
        } else { //In case of intent not resolved even if the app is installed
            handleAppNotInstalled(context, appLaunchConfig, callback);
        }
    }

    private static void handleAppNotInstalled(Context context, AppLaunchConfig appLaunchConfig, Roots.IRootsEvents callback) throws UnsupportedEncodingException {
        if (appLaunchConfig.isAlwaysOpenPlayStore()) {
            openPlayStore(context, appLaunchConfig, callback);
        } else {
            openFallbackUrl(context, appLaunchConfig, callback);
        }
    }

    private static void openPlayStore(Context context, AppLaunchConfig appLaunchConfig, Roots.IRootsEvents callback) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appLaunchConfig.getTargetAppPackageName())));
        } catch (android.content.ActivityNotFoundException ex) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appLaunchConfig.getTargetAppPackageName())));
        } finally {
            if (callback != null) {
                callback.onPlayStoreOpened(appLaunchConfig.getTargetAppName(), appLaunchConfig.getTargetAppPackageName());
            }
        }
    }


    public static void openFallbackUrl(final Context context, final AppLaunchConfig appLaunchConfig, final Roots.IRootsEvents callback) throws UnsupportedEncodingException {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(appLaunchConfig.getTargetAppFallbackUrl()));
        context.startActivity(i);

        if (callback != null) {
            callback.onFallbackUrlOpened(appLaunchConfig.getTargetAppFallbackUrl());
        }
    }

    /**
     * Check if a specified app is configured for Android app link with the given url and launches the app.
     * Return true if app is opened on finding a matching app link
     *
     * @param context         Application context
     * @param url             The url to check for app link configuration
     * @param appLaunchConfig AppLaunchConfig object
     * @return {@link Boolean} with value true if app is launched on finding an app link match
     */
    private static boolean launchOnAppLinkMatchingForUrl(Context context, String url, AppLaunchConfig appLaunchConfig, Roots.IRootsEvents callback) {
        boolean resolvedIntent = false;
        Uri uri = Uri.parse(url);
        if (uri.getScheme() != null) {
            // Check if a possible App link. Android App link urls only support schemes https and http
            if ((uri.getScheme().equalsIgnoreCase("https") || uri.getScheme().equalsIgnoreCase("http"))
                    && !TextUtils.isEmpty(uri.getHost())) {

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setPackage(appLaunchConfig.getTargetAppPackageName());
                intent.setData(uri);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                    if (callback != null) {
                        callback.onAppLaunched(appLaunchConfig.getTargetAppName(), appLaunchConfig.getTargetAppPackageName());
                    }
                    resolvedIntent = true;
                }
            }
        }
        return resolvedIntent;
    }

    /**
     * <p>
     * Checks for any app match for the given url by resolving the URI without a package name.
     * Browsers are removed from the resolved info. Launches the intent only if there is only one application other than
     * browsers is matched for the resolved info
     * </p>
     *
     * @param context  Application context
     * @param url      Url to resolve to installed app
     * @param callback {@link Roots.IRootsEvents} instance to callback resolve url status
     * @return {@link Boolean} with value true if url is resolved to an app.
     */
    public static boolean resolveUrlToAppWithoutPackageName(Context context, String url, Roots.IRootsEvents callback) {
        boolean isAppResolved = false;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        final List<ResolveInfo> matchingApps = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<ResolveInfo> nonBrowserAppResolveInfo = new ArrayList<>();
        // Remove the browser apps form resolved info
        for (ResolveInfo resolveInfo : matchingApps) {
            if (resolveInfo.activityInfo != null
                    && !resolveInfo.activityInfo.packageName.toLowerCase().contains(".browser")
                    && !resolveInfo.activityInfo.packageName.toLowerCase().contains(".chrome")) {
                nonBrowserAppResolveInfo.add(resolveInfo);
            }
        }
        // Launch only if a single app is resolved other than browser apps
        if (nonBrowserAppResolveInfo.size() == 1) {
            isAppResolved = true;
            String packageName = nonBrowserAppResolveInfo.get(0).activityInfo.packageName;
            intent.setPackage(packageName);
            context.startActivity(intent);
            if (callback != null) {
                callback.onAppLaunched(nonBrowserAppResolveInfo.get(0).activityInfo.applicationInfo.name, packageName);
            }
        }

        return isAppResolved;
    }

    /**
     * Checks if app with given package name is available on the device
     *
     * @param context       Current context
     * @param targetPackage Package name of the app to check
     * @return A {@link Boolean} with value true if the given app is installed else false
     */
    private static boolean isAppInstalled(Context context, String targetPackage) {
        boolean isAppInstalled = false;
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
            if (info != null) {
                isAppInstalled = true;
            }
        } catch (PackageManager.NameNotFoundException ignore) {
        }
        return isAppInstalled;
    }


}
