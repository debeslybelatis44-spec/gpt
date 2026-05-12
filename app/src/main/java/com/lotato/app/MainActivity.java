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
    private WebView printWebView;

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
        settings.setDisplayZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ✅ Injecter l'interface AndroidPrint accessible depuis cartManager.js
        webView.addJavascriptInterface(new PrintBridge(), "AndroidPrint");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("https://lotato1.onrender.com/agent1.html");
    }

    // ✅ Bridge appelé par window.AndroidPrint.printHTML(html) dans cartManager.js
    public class PrintBridge {

        @JavascriptInterface
        public void printHTML(final String html) {
            runOnUiThread(() -> {

                // WebView invisible pour charger le HTML du ticket
                printWebView = new WebView(MainActivity.this);
                WebSettings ps = printWebView.getSettings();
                ps.setJavaScriptEnabled(true);
                ps.setDomStorageEnabled(true);

                printWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // Déclencher l'impression Android native
                        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        PrintDocumentAdapter adapter = view.createPrintDocumentAdapter("Ticket LOTATO");

                        PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(new PrintAttributes.MediaSize(
                                "THERMAL_80MM", "Thermal 80mm",
                                3150,  // 80mm en milli-inches
                                8000   // hauteur auto
                            ))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();

                        printManager.print("Ticket LOTATO", adapter, attrs);
                    }
                });

                // Charger le HTML avec la base URL du serveur
                // pour que les images (logo) se chargent correctement
                printWebView.loadDataWithBaseURL(
                    "https://lotato1.onrender.com",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                );
            });
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

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        if (printWebView != null) {
            printWebView.destroy();
        }
        super.onDestroy();
    }
}
