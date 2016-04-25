package io.branch.appconnector;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Branch on 4/6/16.
 * <p>
 * Class for extracting the app connection params such as app links or other metadata for a given url.
 * </p>
 */
class AppConnectionExtractor {

    public enum CONN_EXTRACT_ERR {
        NO_ERROR,
        ERR_NO_INTERNET,
        ERR_UNKNOWN
    }

    // AA: Do we need to give credit to Facebook for this? Let's make sure to cite our sources if so
    private static final String METADATA_READ_JAVASCRIPT = "javascript:window.HTMLOUT.showHTML" +
            "((function() {" +
            "  var metaTags = document.getElementsByTagName('meta');" +
            "  var results = [];" +
            "  for (var i = 0; i < metaTags.length; i++) {" +
            "    var property = metaTags[i].getAttribute('property');" +
            "    if (property && property.substring(0, 'al:'.length) === 'al:') {" +
            "      var tag = { \"property\": metaTags[i].getAttribute('property') };" +
            "      if (metaTags[i].hasAttribute('content')) {" +
            "        tag['content'] = metaTags[i].getAttribute('content');" +
            "      }" +
            "      results.push(tag);" +
            "    }" +
            "  }" +
            "  return JSON.stringify(results);" +
            "})())";


    /**
     * Method for extracting the app link data for a  given url. App link data is scraped from the URL
     * and an {@link AppLaunchConfig} object is created and returned with the callback.
     *
     * @param context  Application context
     * @param url      The Url to open the app
     * @param callback A {@link io.branch.appconnector.AppConnectionExtractor.IAppConnectionExtractorEvents} object for result callback
     */
    public static void scrapeAppLinkTags(final Context context, final String url, String browserAgentString, final IAppConnectionExtractorEvents callback) {
        try {
            final WebView browser = new WebView(context);
            browser.setVisibility(View.GONE);

            browser.getSettings().setJavaScriptEnabled(true);
            browser.getSettings().setBlockNetworkImage(true);
            browser.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            browser.getSettings().setLoadsImagesAutomatically(false);
            browser.getSettings().setAllowContentAccess(false);
            browser.getSettings().setDomStorageEnabled(true);

            browser.getSettings().setUserAgentString(getUserAgentString(context, url, browserAgentString, browser));

            browser.addJavascriptInterface(new Object() {
                @SuppressWarnings("unused")
                @JavascriptInterface
                public void showHTML(String html) throws JSONException {
                    AppLaunchConfig appLaunchConfig = new AppLaunchConfig(new JSONArray(html), url);
                    if (callback != null) {
                        callback.onAppLaunchConfigAvailable(appLaunchConfig, CONN_EXTRACT_ERR.NO_ERROR);
                    }
                }
            }, "HTMLOUT");

            browser.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                   super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    browser.loadUrl(METADATA_READ_JAVASCRIPT);
                }
            });
            browser.loadUrl(url);
        } catch (Exception ex) {
            if (callback != null) {
                AppLaunchConfig appLaunchConfig = new AppLaunchConfig(null, url);
                callback.onAppLaunchConfigAvailable(appLaunchConfig, CONN_EXTRACT_ERR.ERR_UNKNOWN);
            }

        }
    }


    public interface IAppConnectionExtractorEvents {
        /**
         * Called when AppLaunch config is created for a given url
         *
         * @param appLaunchConfig {@link AppLaunchConfig} instance for teh given url
         * @param err             {@link io.branch.appconnector.AppConnectionExtractor.CONN_EXTRACT_ERR} representing any error while creating app launch config
         */
        void onAppLaunchConfigAvailable(AppLaunchConfig appLaunchConfig, CONN_EXTRACT_ERR err);
    }

    private static String getUserAgentString(Context context, String url, String customUserAgentString, WebView view) {
        // Check if the url is a Branch Url
        // AA: We're going to need to get rid of this code. You can update the backend to check for 'app connector'. It's fine to leave this for the time being though.
        try {
            Uri uri = Uri.parse(url);
            if (uri.getHost().equalsIgnoreCase("bnc.lt")
                    || uri.getHost().toLowerCase().endsWith(".app.link")) {
                String packageName = context.getApplicationContext().getPackageName();
                String sdkVersion = Defines.VERSION_NAME;
                return "<" + packageName + " app connector " + sdkVersion + ">";
            }
        } catch (Exception ignore) {
        }

        String uaString = customUserAgentString != null ? customUserAgentString : view.getSettings().getUserAgentString();
        String packageName = context.getApplicationContext().getPackageName();
        String sdkVersion = Defines.VERSION_NAME;
        uaString = uaString + " " + packageName + " app connector " + sdkVersion;
        return uaString;

    }


}
