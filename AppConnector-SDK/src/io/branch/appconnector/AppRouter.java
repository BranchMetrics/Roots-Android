package io.branch.appconnector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;

/**
 * Created by sojanpr on 4/7/16.
 * <p>
 * Class for handling the routing with the given {@link AppLaunchConfig}.
 * Try to open a matching app if installed otherwise fallback to play store or web url.
 * </p>
 */
class AppRouter {


    public static boolean handleAppRouting(Context context, AppLaunchConfig appLaunchConfig, AppConnector.IAppConnectionEvents callback) {
        boolean routingHandled = true;
        try {
            if (appLaunchConfig.isLaunchIntentAvailable()) {
                if (isAppInstalled(context, appLaunchConfig.getTargetAppPackageName())) {  //Open the app if app is installed
                    openAppWithUriScheme(context, appLaunchConfig, callback);
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


    private static void openAppWithUriScheme(Context context, AppLaunchConfig appLaunchConfig, AppConnector.IAppConnectionEvents callback) throws UnsupportedEncodingException {
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

    private static void handleAppNotInstalled(Context context, AppLaunchConfig appLaunchConfig, AppConnector.IAppConnectionEvents callback) throws UnsupportedEncodingException {
        if(appLaunchConfig.isAlwaysOpenWebUrl()){
            openFallbackUrl(context,appLaunchConfig,callback);
        }else {
            openPlayStore(context, appLaunchConfig, callback);
        }
    }

    private static void openPlayStore(Context context, AppLaunchConfig appLaunchConfig, AppConnector.IAppConnectionEvents callback) {
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


    private static void openFallbackUrl(Context context, AppLaunchConfig appLaunchConfig, AppConnector.IAppConnectionEvents callback) throws UnsupportedEncodingException {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(appLaunchConfig.getTargetAppFallbackUrl()));
        context.startActivity(i);

        if (callback != null) {
            callback.onFallbackUrlOpened(appLaunchConfig.getTargetAppFallbackUrl());
        }
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
