package com.lotato.app;

import android.app.Activity;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.content.Context;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        // Interface Java accessible depuis JavaScript
        webView.addJavascriptInterface(new PrintInterface(this, webView), "AndroidPrint");

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, android.os.Message resultMsg) {
                // Ouvrir les popups dans la même WebView
                WebView newWebView = new WebView(view.getContext());
                WebSettings s = newWebView.getSettings();
                s.setJavaScriptEnabled(true);
                s.setDomStorageEnabled(true);

                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView w) {
                        // Fermer la popup après impression
                    }
                });

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // Déclencher l'impression Android native
                        triggerPrint(view);
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.loadUrl("https://lotato1.onrender.com/agent1.html");
    }

    private void triggerPrint(WebView targetView) {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter adapter = targetView.createPrintDocumentAdapter("Ticket LOTATO");
        PrintAttributes attrs = new PrintAttributes.Builder()
                .setMediaSize(new PrintAttributes.MediaSize(
                        "THERMAL_80MM", "Thermal 80mm", 3150, 8000))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        printManager.print("Ticket LOTATO", adapter, attrs);
    }

    // Interface JavaScript → Android
    public class PrintInterface {
        Context context;
        WebView webView;

        PrintInterface(Context c, WebView w) {
            this.context = c;
            this.webView = w;
        }

        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> triggerPrint(webView));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
