package io.branch.appconnector;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Branch on 4/6/16.
 * <p>
 * Class for extracting the app connection params such as app links or other metadata for a given url.
 * </p>
 */
class AppConnectionExtractor {

    public enum CONN_EXTRACT_ERR {
        NO_ERROR,
        ERR_UNKNOWN
    }

    // Injecting Javascript to get the app links as JSONArray
    // Source :https://github.com/BoltsFramework/Bolts-Android/blob/master/bolts-applinks/src/main/java/bolts/WebViewAppLinkResolver.java#L52
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
        new CaptureAppLaunchConfigTask(context, url, getUserAgentString(context, url, browserAgentString), callback).execute();
    }

    /**
     * <p>
     * Background task for getting the {@link AppLaunchConfig} for the given url
     * </p>
     */
    private static class CaptureAppLaunchConfigTask extends AsyncTask<Void, Void, URLContent> {
        private final Context context_;
        private final String browserAgentString_;
        private final IAppConnectionExtractorEvents callback_;
        private final String actualUrl_;

        public CaptureAppLaunchConfigTask(Context context, String actualUrl, String browserAgentString, IAppConnectionExtractorEvents callback) {
            context_ = context;
            browserAgentString_ = browserAgentString;
            callback_ = callback;
            actualUrl_ = actualUrl;
        }

        @Override
        protected URLContent doInBackground(Void... params) {
            return getURLContent(actualUrl_, browserAgentString_);
        }

        @Override
        protected void onPostExecute(URLContent urlContent) {
            super.onPostExecute(urlContent);
            captureAppLinkMetaData(context_, urlContent, browserAgentString_, actualUrl_, callback_);
        }
    }


    /**
     * <p>
     * Loads the url and check for any redirection. Extract the app link content form the final redirected
     * url and create an {@link URLContent} instance from the captured html source
     * </p>
     *
     * @param originUrl URL to app connect
     * @param userAgent User agent string
     * @return {@link URLContent} instance with the content of the given url
     */
    private static URLContent getURLContent(String originUrl, String userAgent) {
        URLContent urlContent = null;
        try {
            String finalDestinationURl = originUrl;
            URLConnection urlConnection = null;

            while (!TextUtils.isEmpty(finalDestinationURl)) {
                urlConnection = new URL(originUrl).openConnection();
                urlConnection.setRequestProperty("Prefer-Html-Meta-Tags", "al");
                urlConnection.addRequestProperty("User-Agent", userAgent);

                if (urlConnection instanceof HttpURLConnection) {
                    ((HttpURLConnection) urlConnection).setInstanceFollowRedirects(true);
                }

                urlConnection.connect();
                if (urlConnection instanceof HttpURLConnection) { //Http url connection is the base class for both http and https url connections
                    HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode >= 300 && responseCode < 400) {
                        finalDestinationURl = httpURLConnection.getHeaderField("Location");
                        httpURLConnection.disconnect();
                    } else {
                        finalDestinationURl = null;
                    }
                } else {
                    finalDestinationURl = null;
                }
            }
            urlContent = getURLContentFromConnection(urlConnection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlContent;
    }

    /**
     * <p>
     * Get the Contents of a given URL connection
     * </p>
     *
     * @param urlConnection {@link URLConnection} instance
     * @return {@link URLContent} for teh given connection
     */
    private static URLContent getURLContentFromConnection(URLConnection urlConnection) {
        URLContent urlContent = null;
        try {
            urlContent = new URLContent(urlConnection.getContentType());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            urlContent.setHtmlSource_(outputStream.toString(urlContent.getContentEncoding()));
            outputStream.close();
            inputStream.close();
        } catch (Exception ignore) {
        }
        return urlContent;
    }


    private static void captureAppLinkMetaData(Context context, URLContent content, String browserAgentString, final String actualUrl, final IAppConnectionExtractorEvents callback) {
        try {
            if (content != null && !TextUtils.isEmpty(content.getHtmlSource())) {
                final WebView browser = new WebView(context);
                browser.setVisibility(View.GONE);

                browser.getSettings().setJavaScriptEnabled(true);
                browser.getSettings().setBlockNetworkImage(true);
                browser.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                browser.getSettings().setLoadsImagesAutomatically(false);
                browser.getSettings().setAllowContentAccess(false);
                browser.getSettings().setDomStorageEnabled(true);

                browser.getSettings().setUserAgentString(browserAgentString);

                browser.addJavascriptInterface(new Object() {
                    @SuppressWarnings("unused")
                    @JavascriptInterface
                    public void showHTML(String html) throws JSONException {
                        AppLaunchConfig appLaunchConfig = new AppLaunchConfig(new JSONArray(html), actualUrl);
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

                browser.loadDataWithBaseURL(null, content.getHtmlSource(), content.getContentType(), content.getContentEncoding(), null);
            } else {
                if (callback != null) {
                    AppLaunchConfig appLaunchConfig = new AppLaunchConfig(null, actualUrl);
                    callback.onAppLaunchConfigAvailable(appLaunchConfig, CONN_EXTRACT_ERR.ERR_UNKNOWN);
                }
            }
        } catch (Exception ex) {
            if (callback != null) {
                AppLaunchConfig appLaunchConfig = new AppLaunchConfig(null, actualUrl);
                callback.onAppLaunchConfigAvailable(appLaunchConfig, CONN_EXTRACT_ERR.ERR_UNKNOWN);
            }

        }
    }

    private static String getUserAgentString(Context context, String url, String customUserAgentString) {
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
        WebView webView = new WebView(context);
        String uaString = customUserAgentString != null ? customUserAgentString : webView.getSettings().getUserAgentString();
        String packageName = context.getApplicationContext().getPackageName();
        String sdkVersion = Defines.VERSION_NAME;
        uaString = uaString + " " + packageName + " app connector " + sdkVersion;
        return uaString;

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


}
