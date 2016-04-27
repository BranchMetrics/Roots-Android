package io.branch.appconnectordeeplinktestbed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by sojanpr on 4/12/16.
 * <p>
 *     Main activity for the App Connector deep link test bed. *
 * </p>
 */
public class MainActivity  extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }
}
