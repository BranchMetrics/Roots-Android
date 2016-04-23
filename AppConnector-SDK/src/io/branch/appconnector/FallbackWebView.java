package io.branch.appconnector;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

// AA: Can we delete this class from master? Keep it in a separate branch for V2. Just don't want to confuse people

/**
 * <p>
 * Class for showing the fallback url. Class provides functionality to add a download application button also to help downloading the app.
 * </p>
 */
class FallbackWebView extends WebView {
    private final AppLaunchConfig appLaunchConfig_;
    private final Context context_;
    private IFallbackWebViewActionEvents callback_;

    public FallbackWebView(Context context, AppLaunchConfig appLaunchConfig, IFallbackWebViewActionEvents callback) {
        super(context);
        context_ = context;
        appLaunchConfig_ = appLaunchConfig;
        callback_ = callback;
    }

    public void createAndShow() {
        if (context_ != null) {
            WebView webView = new WebView(context_);
            final RelativeLayout layout = new RelativeLayout(context_);
            layout.setBackgroundColor(Color.parseColor("#11FEFEFE"));
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(3, 3, 3, 3);

            layout.addView(webView, layoutParams);

            // TODO need more customisation to show application icon  or a Play Store Badge
            if (!TextUtils.isEmpty(appLaunchConfig_.getTargetAppPackageName())
                    && appLaunchConfig_.isAddDownloadAppBtn()) {
                TextView downloadTextView = new TextView(context_);
                downloadTextView.setText("Download " + appLaunchConfig_.getTargetAppName() + " App");
                downloadTextView.setTextAppearance(context_, android.R.style.TextAppearance_Medium);
                downloadTextView.setBackgroundColor(Color.parseColor("#FFFF3434"));
                downloadTextView.setPadding(10, 15, 10, 15);
                downloadTextView.setGravity(Gravity.CENTER);

                RelativeLayout.LayoutParams downloadBtnlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                downloadBtnlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                layout.addView(downloadTextView, downloadBtnlp);

                downloadTextView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (callback_ != null) {
                            callback_.onDownloadAppSelected();
                        }
                    }
                });

            }
            if (Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            webView.loadUrl(appLaunchConfig_.getTargetAppFallbackUrl());
            final Dialog dialog = new Dialog(context_, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(layout);
            dialog.show();

            webView.getSettings().setJavaScriptEnabled(true);
        }
    }

    public interface IFallbackWebViewActionEvents {
        void onDownloadAppSelected();
    }
}
