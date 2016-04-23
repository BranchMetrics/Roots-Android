package io.branch.appconnector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created by sojanpr on 4/14/16.
 * <p>
 *     Utility class for checking the URI match with given pattern and creating target intents
 * </p>
 */
class Matcher {

    // AA: For notes only: please add the * variable to path functionality

    public static boolean matchUriPattern(String actualUriString, String pattern) {
        String paramName = getParamNameFromUriPattern(pattern);
        String patternWithoutParam = pattern.replace("{" + paramName + "}", "").toLowerCase();
        return (actualUriString.toLowerCase().startsWith(patternWithoutParam));
    }

    public static String getParamNameFromUriPattern(String pattern) {
        return pattern.replaceAll(".*\\{|\\}.*", "");
    }

    public static Intent createTargetIntent(Activity parentActivity, String targetActivityName, String actualUriString, String pattern) throws ClassNotFoundException {
        Intent intent = new Intent(parentActivity, Class.forName(targetActivityName));
        intent.putExtra(Defines.APP_CONNECTOR_DEEPLINK_LAUNCH_KEY, true);
        String paramName = getParamNameFromUriPattern(pattern);
        if (!TextUtils.isEmpty(paramName)) {
            String queryParams = Uri.parse(actualUriString).getQuery();
            if (queryParams != null) {
                actualUriString = actualUriString.replaceAll("\\?" + queryParams.toLowerCase(), "");
            }
            String patternWithoutParam = pattern.replace("{" + paramName + "}", "").toLowerCase();
            String paramVal = actualUriString.replaceAll(patternWithoutParam, "");
            intent.putExtra(paramName, paramVal);
        }
        return intent;
    }
}
