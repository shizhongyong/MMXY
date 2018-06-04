package cn.boweikeji.wuliu.webview;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long EXIT_INTERVAL = 2000;

    private static final String TEL = "tel:";

    private JsInterface.CallBack mJsInterfaceCallBack = new JsInterface.CallBack() {
        @Override
        public void invokeJsMethod(String method, String data) {
            if (TextUtils.isEmpty(method)) {
                return;
            }

            StringBuilder builder = new StringBuilder();
            builder.append("javascript:");
            builder.append(method);
            builder.append("(\"");
            if (data != null) {
                builder.append(data);
            }
            builder.append("\")");
            mWebView.loadUrl(builder.toString());
        }
    };

    private WebView mWebView;
    private ProgressBar mProgressBar;

    private CustomChromeClient mWebChromeClient;
    private JsInterface mJsInterface;

    private long mLastBackTime;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mWebChromeClient != null) {
            mWebChromeClient.onFileChooserResult(requestCode, resultCode, data);
        }
        if (mJsInterface != null) {
            mJsInterface.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.webview);
        mProgressBar = findViewById(R.id.progress);

        initWebView();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        if (System.currentTimeMillis() - mLastBackTime > EXIT_INTERVAL) {
            Toast.makeText(this, R.string.msg_exit_app, Toast.LENGTH_SHORT).show();
            mLastBackTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        try {
            // 先清空，再删除
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();

            ViewGroup parent = (ViewGroup) mWebView.getParent();
            parent.removeView(mWebView);
            mWebView.destroy();
            mWebView = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void initWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);

        mJsInterface = new JsInterface(this, mJsInterfaceCallBack);
        mWebView.addJavascriptInterface(mJsInterface, JsInterface.NAME);

        mWebChromeClient = new CustomChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return onInterceptUrl(url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        mWebView.loadUrl(getString(R.string.home_page));
    }

    public boolean onInterceptUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        url = url.toLowerCase();
        if (url.startsWith(TEL)) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
            return true;
        }

        return false;
    }

    private class CustomChromeClient extends WebChromeClient {

        private static final int RC_CHOOSE_FILE = 0x1234;

        private ValueCallback<Uri> mUploadFile;
        private ValueCallback<Uri[]> mUploadMultiFiles;

        @Override
        public void onReceivedTitle(WebView view, String title) {
            Log.d(TAG, "onReceivedTitle: " + title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mProgressBar.setProgress(newProgress);
        }

        // 3.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
            openFileChooser(uploadFile, acceptType, null);
        }

        // 4.1.2
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
            mUploadFile = uploadFile;
            openFileChooserActivity(acceptType);
        }

        // 5.0
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadMultiFiles = filePathCallback;
            openFileChooserActivity(fileChooserParams.getAcceptTypes());
            return true;
        }

        private void openFileChooserActivity(String... acceptTypes) {
            String acceptType = "image/*";
            if (acceptTypes != null && acceptTypes.length > 0) {
                acceptType = acceptTypes[0];
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(acceptType);
            startActivityForResult(Intent.createChooser(intent, "Chooser"), RC_CHOOSE_FILE);
        }

        private void onFileChooserResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == RC_CHOOSE_FILE) {
                if (mUploadMultiFiles != null) {
                    Uri[] uris = null;
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        String dataString = data.getDataString();
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            uris = new Uri[clipData.getItemCount()];
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                uris[i] = clipData.getItemAt(i).getUri();
                            }
                        }
                        if (dataString != null) {
                            uris = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    mUploadMultiFiles.onReceiveValue(uris);
                    mUploadMultiFiles = null;
                } else if (mUploadFile != null) {
                    Uri uri = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                    mUploadFile.onReceiveValue(uri);
                    mUploadFile = null;
                }
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
            super.onGeolocationPermissionsShowPrompt(origin, callback);
        }
    }

}
