package io.branch.appconnectortestbed;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import io.branch.appconnector.AppConnector;

/**
 * <p>
 * Class to demonstrate and test the AppConnector sdk. This app will open any app using the url
 * provided if app is installed. Fall back to a play store or a website is app is not installed.
 * Also show how to debug AppConnector SDK with debug app link data
 * </p>
 */
public class TestBedMainActivity extends Activity implements AppConnector.IAppConnectionEvents {
    ProgressDialog progressDialog_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bed_main);
        final TextView utlTxtView = (TextView) findViewById(R.id.app_url_txt);
        utlTxtView.setText("Enter the url to open app");

        findViewById(R.id.app_nav_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog_ = ProgressDialog.show(TestBedMainActivity.this, utlTxtView.getText().toString(), "Opening", true);
                progressDialog_.setCancelable(true);
                String navUrl = utlTxtView.getText().toString();

                AppConnector appConnector = new AppConnector(TestBedMainActivity.this, navUrl)
                        .setAppConnectionEventsCallback(TestBedMainActivity.this);

                if (URLUtil.isValidUrl(navUrl)) {
                    appConnector.connect(); // Open the using the URL entered
                } else {     // Set debug data to open appconnectordeeplinktestbed app
                    try {
                        appConnector.debugConnect("https://my_awesome_site.com/user/my_user_id123456",
                                new JSONArray("[{\"property\":\"al:android:url\"," +
                                        "\"content\":\"myscheme://user/my_user_id1234\"}," +
                                        "{\"property\":\"al:android:package\",\"content\":\"io.branch.appconnectordeeplinktestbed\"}," +
                                        "{\"property\":\"al:android:app_name\",\"content\":\"AppConnectorTestBed\"}]"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }

    @Override
    public void onAppLaunched(String appName, String packageName) {
        if (progressDialog_ != null) {
            progressDialog_.dismiss();
        }
    }

    @Override
    public void onFallbackUrlOpened(String url) {
        if (progressDialog_ != null) {
            progressDialog_.dismiss();
        }
    }

    @Override
    public void onPlayStoreOpened(String appName, String packageName) {
        if (progressDialog_ != null) {
            progressDialog_.dismiss();
        }
    }
}
