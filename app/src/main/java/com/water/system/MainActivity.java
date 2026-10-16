package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient.FileChooserParams;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // تفعيل JavaScript
        settings.setJavaScriptEnabled(true);

        // تفعيل التخزين المحلي
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // السماح بتحميل الملفات المحلية من مجلد assets
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // إعداد عرض التطبيق على الهاتف
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // السماح بالاتصالات الآمنة وغير الآمنة عند الحاجة
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );

        // التعامل مع الروابط داخل التطبيق
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if (scheme != null &&
                        (scheme.equals("http") ||
                         scheme.equals("https") ||
                         scheme.equals("file") ||
                         scheme.equals("content"))) {

                    if ("http".equals(scheme) ||
                            "https".equals(scheme)) {

                        String host = uri.getHost();

                        if (host != null &&
                                !host.equals("accounts.google.com") &&
                                !host.equals("google.com") &&
                                !host.endsWith(".google.com")) {

                            try {
                                Intent intent = new Intent(
                                        Intent.ACTION_VIEW, uri
                                );
                                startActivity(intent);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    }

                    return false;
                }

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "لا يمكن فتح هذا الرابط",
                            Toast.LENGTH_SHORT
                    ).show();
                    return true;
                }
            }
        });

        // دعم اختيار الصور والملفات من صفحات HTML
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }

                fileChooserCallback = filePathCallback;

                try {
                    Intent intent =
                            fileChooserParams.createIntent();

                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                    );

                } catch (Exception e) {

                    fileChooserCallback = null;

                    Toast.makeText(
                            MainActivity.this,
                            "تعذر فتح اختيار الملفات",
                            Toast.LENGTH_LONG
                    ).show();

                    return false;
                }

                return true;
            }
        });

        // فتح ملف التطبيق من مجلد assets
        webView.loadUrl("file:///android_asset/index.html");
    }

    // استلام الملف الذي اختاره المستخدم
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (fileChooserCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        results[i] =
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri();
                    }

                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
                }
            }

            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    // زر الرجوع في الهاتف
    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // تحرير الموارد عند إغلاق التطبيق
    @Override
    protected void onDestroy() {

        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}