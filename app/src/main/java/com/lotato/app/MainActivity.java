package com.lotato.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private String lastPrintedHtml = "";  // Évite les impressions en double

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
        settings.setAllowFileAccess(false);  // Sécurité : pas d'accès aux fichiers locaux

        webView.addJavascriptInterface(new PrintBridge(), "AndroidPrint");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Patch JavaScript amélioré : gestion du délai glissant et impression unique
                String patch =
                        "(function() {" +
                        "    if (window.__printPatched) return;" +
                        "    window.__printPatched = true;" +
                        "    var _originalOpen = window.open;" +
                        "    window.open = function(url, name, features) {" +
                        "        var buffer = '';" +
                        "        var printTimer = null;" +
                        "        var alreadyPrinted = false;" +
                        "        var popup = {" +
                        "            document: {" +
                        "                write: function(html) {" +
                        "                    buffer += html;" +
                        "                    if (printTimer) clearTimeout(printTimer);" +
                        "                    // Délai après la dernière écriture (800 ms, ajustable)" +
                        "                    printTimer = setTimeout(function() {" +
                        "                        if (!alreadyPrinted && window.AndroidPrint && buffer.trim().length > 0) {" +
                        "                            alreadyPrinted = true;" +
                        "                            window.AndroidPrint.printHTML(buffer);" +
                        "                        }" +
                        "                    }, 800);" +
                        "                }," +
                        "                writeln: function(html) { this.write(html + '\\n'); }," +
                        "                close: function() {" +
                        "                    if (printTimer) clearTimeout(printTimer);" +
                        "                    if (!alreadyPrinted && window.AndroidPrint && buffer.trim().length > 0) {" +
                        "                        alreadyPrinted = true;" +
                        "                        window.AndroidPrint.printHTML(buffer);" +
                        "                    }" +
                        "                }" +
                        "            }," +
                        "            close: function() { this.document.close(); }," +
                        "            focus: function() {}," +
                        "            onload: null" +
                        "        };" +
                        "        return popup;" +
                        "    };" +
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
            // Éviter les impressions multiples strictement identiques
            if (html.equals(lastPrintedHtml)) {
                Log.d("PrintBridge", "Ignored duplicate print (same HTML)");
                return;
            }
            lastPrintedHtml = html;
            Log.d("PrintBridge", "Printing HTML, length: " + html.length());

            runOnUiThread(() -> {
                // WebView temporaire pour l'impression (invisible)
                WebView printWebView = new WebView(MainActivity.this);
                printWebView.getSettings().setJavaScriptEnabled(true);

                printWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        PrintDocumentAdapter adapter = view.createPrintDocumentAdapter("Ticket LOTATO");

                        // Dimensions correctes pour ticket 80 mm
                        PrintAttributes attrs = new PrintAttributes.Builder()
                                .setMediaSize(new PrintAttributes.MediaSize(
                                        "thermal_80mm", "Thermal 80mm",
                                        80000,   // 80 mm en microns
                                        300000   // hauteur 300 mm (suffisante)
                                ))
                                .setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0))
                                .build();

                        printManager.print("Ticket LOTATO", adapter, attrs);

                        // Libérer la WebView après l'impression (éviter fuite mémoire)
                        view.postDelayed(() -> view.destroy(), 2000);
                    }
                });

                // Charger le HTML avec la bonne base URL pour les ressources relatives
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
