package io.branch.roots;

import android.app.Activity;
import android.content.Intent;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by sojanpr on 4/14/16.
 * <p>
 * Utility class for checking the URI match with given pattern and creating target intents
 * </p>
 */
class Matcher {

    /**
     * Check if the given url is matching the pattern.
     * Pattern can specify variables with "{}" and wild cards with "*"
     *
     * @param uri     Uri to check the matching
     * @param pattern Pattern to check the match
     * @return {@link Boolean} whose value is true if the URI matches the given pattern.
     */
    public static boolean checkUriMatchForPattern(String uri, String pattern) {
        boolean isMatch = false;
        try {
            //TODO check for capture
            String getValueExpression = pattern.replaceAll("(\\{[^}]*\\})", "(.+)"); // Replace all variables with capture expression
            getValueExpression = getValueExpression.replaceAll("\\*", ".+"); // Replace all wild cards with capture expression
            isMatch = Pattern.compile(getValueExpression).matcher(uri).matches();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isMatch;
    }

    public static Intent createTargetIntent(Activity parentActivity, String targetActivityName, String actualUriString, String pattern) throws ClassNotFoundException {
        Intent intent = new Intent(parentActivity, Class.forName(targetActivityName));
        intent.putExtra(Defines.APP_CONNECTOR_DEEPLINK_LAUNCH_KEY, true);

        HashMap<String, String> paramValMap = captureParamsFromUri(actualUriString, pattern);
        for (String paramName : paramValMap.keySet()) {
            intent.putExtra(paramName, paramValMap.get(paramName));
        }
        return intent;
    }

    /**
     * Create a Map with param names and values captures from pattern and uri respectively.
     * Params are represented in the pattern with in "{}". This method get these param names and their corresponding
     * matching value in the url
     *
     * @param uri     Uri to get matching values for the params specified in pattern with in "{}"
     * @param pattern Pattern string with param names in "{}"
     * @return A map with param names and corresponding values
     */
    private static HashMap<String, String> captureParamsFromUri(String uri, String pattern) {
        HashMap<String, String> paramValueMap = new HashMap<>();

        String uriWithoutQueryParam = uri.split("\\?")[0];
        String getValueExpression = pattern.replaceAll("\\*", ".+");
        getValueExpression = getValueExpression.replaceAll("(\\{[^}]*\\})", "(.+)");
        java.util.regex.Matcher valueMatcher = Pattern.compile(getValueExpression).matcher(uriWithoutQueryParam);

        String getParamExpression = pattern.replaceAll("\\*", ".+");
        getParamExpression = getParamExpression.replaceAll("(\\{[^/]*\\})", "\\\\{(.*?)\\\\}");
        java.util.regex.Matcher paramMatcher = Pattern.compile(getParamExpression).matcher(pattern);

        if (paramMatcher.matches() && valueMatcher.matches()) {
            for (int i = 0; i < paramMatcher.groupCount(); i++) {
                try {
                    String paramStr = paramMatcher.group(i + 1);
                    String valueStr = valueMatcher.group(i + 1);
                    paramValueMap.put(paramStr, valueStr);
                } catch (IllegalStateException ignore) {
                }
            }
        }
        return paramValueMap;
    }

}
