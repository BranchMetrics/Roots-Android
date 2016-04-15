package io.branch.appconnector;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by sojanpr on 4/6/16.
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

    private static final String USER_AGENT_STRING = "Chrome 41.0.2227.1";
    //private static final String USER_AGENT_STRING = "Mozilla/5.0 (Linux; U; Android 4.0.3; de-ch; HTC Sensation Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

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
     * @param context  Application context
     * @param url   The Url to open the app
     * @param callback A {@link io.branch.appconnector.AppConnectionExtractor.IAppConnectionExtractorEvents} object for result callback
     */
    public static void scrapeAppLinkTags(final Context context, final String url, final IAppConnectionExtractorEvents callback) {
        try {
            final WebView browser = new WebView(context);
            browser.getSettings().setJavaScriptEnabled(true);
            browser.getSettings().setUserAgentString(USER_AGENT_STRING);

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
         * @param appLaunchConfig {@link AppLaunchConfig} instance for teh given url
         * @param err {@link io.branch.appconnector.AppConnectionExtractor.CONN_EXTRACT_ERR} representing any error while creating app launch config
         */
        void onAppLaunchConfigAvailable(AppLaunchConfig appLaunchConfig, CONN_EXTRACT_ERR err);
    }


}
