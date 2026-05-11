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
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new PrintBridge(), "AndroidPrint");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Injecter le patch qui remplace window.open par AndroidPrint
                String patch = 
                    "(function() {" +
                    "  var _originalOpen = window.open;" +
                    "  window.open = function(url, name, features) {" +
                    "    var popup = {" +
                    "      document: {" +
                    "        _html: ''," +
                    "        write: function(html) { this._html += html; }," +
                    "        close: function() {" +
                    "          if (window.AndroidPrint) {" +
                    "            window.AndroidPrint.printHTML(this._html);" +
                    "          }" +
                    "        }" +
                    "      }," +
                    "      focus: function() {}," +
                    "      close: function() {}," +
                    "      onload: null," +
                    "      print: function() {}" +
                    "    };" +
                    "    setTimeout(function() {" +
                    "      if (popup.onload) popup.onload();" +
                    "    }, 500);" +
                    "    return popup;" +
                    "  };" +
                    "})();";
                view.evaluateJavascript(patch, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://lotato1.onrender.com/agent1.html");
    }

    public class PrintBridge {
        @JavascriptInterface
        public void printHTML(final String html) {
            runOnUiThread(() -> {
                // Créer une WebView invisible pour charger le HTML du ticket
                printWebView = new WebView(MainActivity.this);
                printWebView.getSettings().setJavaScriptEnabled(true);

                printWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // Lancer l'impression Android native
                        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        PrintDocumentAdapter adapter = view.createPrintDocumentAdapter("Ticket LOTATO");
                        PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(new PrintAttributes.MediaSize(
                                "THERMAL_80MM", "Thermal 80mm", 3150, 8000))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                        printManager.print("Ticket LOTATO", adapter, attrs);
                    }
                });

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
}
