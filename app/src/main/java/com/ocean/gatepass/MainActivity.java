package com.ocean.gatepass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int FILE_CHOOSER_REQUEST      = 101;
    private static final String TARGET_URL =
            "https://oceanfibers.com.pk/gatepass/verify.php";

    private WebView     webView;
    private LinearLayout errorLayout;
    private LinearLayout loadingLayout;
    private TextView    errorMessage;
    private ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView       = findViewById(R.id.webView);
        errorLayout   = findViewById(R.id.errorLayout);
        loadingLayout = findViewById(R.id.loadingLayout);
        errorMessage  = findViewById(R.id.errorMessage);
        Button retryBtn = findViewById(R.id.retryButton);

        setupWebView();
        requestCameraPermission();

        retryBtn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                errorLayout.setVisibility(View.GONE);
                loadingLayout.setVisibility(View.VISIBLE);
                webView.reload();
            } else {
                showError("انٹرنیٹ کنیکشن نہیں\nنیٹ ورک چیک کریں");
            }
        });

        loadPage();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Disguise as Chrome Mobile
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new OceanWebViewClient());
        webView.setWebChromeClient(new OceanChromeClient());
    }

    private void loadPage() {
        if (isNetworkAvailable()) {
            webView.loadUrl(TARGET_URL);
        } else {
            loadingLayout.setVisibility(View.GONE);
            showError("انٹرنیٹ کنیکشن نہیں\nنیٹ ورک چیک کریں");
        }
    }

    private void showError(String msg) {
        webView.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        errorMessage.setText(msg);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
            (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int req,
            @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == CAMERA_PERMISSION_REQUEST
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            webView.reload();
        }
    }

    // ---- WebViewClient ----
    private class OceanWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView v, String url, Bitmap fav) {
            super.onPageStarted(v, url, fav);
            loadingLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.INVISIBLE);
            errorLayout.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView v, String url) {
            super.onPageFinished(v, url);
            loadingLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req,
                WebResourceError err) {
            if (req.isForMainFrame()) {
                String msg;
                int code = err.getErrorCode();
                if (code == ERROR_HOST_LOOKUP || code == ERROR_NAME_NOT_RESOLVED) {
                    msg = "سرور نہیں ملا\nانٹرنیٹ چیک کریں";
                } else if (code == ERROR_TIMEOUT) {
                    msg = "کنیکشن ٹائم آؤٹ\nدوبارہ کوشش کریں";
                } else if (code == ERROR_CONNECT || code == ERROR_IO) {
                    msg = "سرور سے کنیکشن نہیں\nکچھ دیر بعد کوشش کریں";
                } else {
                    msg = "ایک خرابی آئی ہے\nدوبارہ کوشش کریں";
                }
                showError(msg);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v,
                WebResourceRequest req) {
            String url = req.getUrl().toString();
            if (url.contains("oceanfibers.com.pk")) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,
                    "لنک نہیں کھل سکا", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    }

    // ---- WebChromeClient ----
    private class OceanChromeClient extends WebChromeClient {

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            for (String res : request.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res)) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        requestCameraPermission();
                        request.deny();
                    }
                    return;
                }
            }
            request.grant(request.getResources());
        }

        @Override
        public boolean onShowFileChooser(WebView wv,
                ValueCallback<Uri[]> cb, FileChooserParams params) {
            filePathCallback = cb;
            try {
                startActivityForResult(params.createIntent(),
                    FILE_CHOOSER_REQUEST);
            } catch (Exception e) {
                filePathCallback = null;
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            filePathCallback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(res, data));
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onResume() { super.onResume(); webView.onResume(); }
    @Override protected void onPause()  { super.onPause();  webView.onPause();  }
}
