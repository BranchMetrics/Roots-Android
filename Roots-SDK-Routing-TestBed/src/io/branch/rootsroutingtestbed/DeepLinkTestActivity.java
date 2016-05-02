package io.branch.rootsroutingtestbed;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * <p>
 * Activity for testing In-App-Routing with AppConnector SDK. The URL filtering rule and
 * param names are specified as a metadata for this Activity in manifest. Upon seeing a match with
 * app link url or with a fallback url Appconnector SDK launches this activity. The parameter name in the url format is
 * specified in the filter as "{param name}" ("myscheme://user/{user_id}"). App connector will extract the param value from
 * the URL and add it to intent as value for param name.
 * <p/>
 * </p>
 */
public class DeepLinkTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deeplinktest);

        Bundle bundle = getIntent().getExtras();
        String paramString = "";
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            paramString += key + "  =  " + value + "\n";

        }
        ((TextView) findViewById(R.id.params_txt)).setText(paramString);
    }
}
