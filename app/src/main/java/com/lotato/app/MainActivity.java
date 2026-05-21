package com.lotato.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;

import woyou.aidlservice.jiuiv5.IWoyouService;

public class MainActivity extends Activity {

    private static final String TAG = "LOTATO_PRINT";
    private WebView webView;
    private IWoyouService woyouService;
    private boolean printerConnected = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            printerConnected = true;
            Log.d(TAG, "✅ Service Sunmi connecté");
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.evaluateJavascript("window.sunmiPrinterReady = true;", null);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            woyouService = null;
            printerConnected = false;
            Log.d(TAG, "❌ Service Sunmi déconnecté");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindSunmiService();

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

        webView.addJavascriptInterface(new SunmiBridge(), "AndroidPrint");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://lotato1.onrender.com/agent1.html");
    }

    private void bindSunmiService() {
        Intent intent = new Intent();
        intent.setPackage("woyou.aidlservice.jiuiv5");
        intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
        boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Liaison service Sunmi: " + bound);
    }

    public class SunmiBridge {

        @JavascriptInterface
        public void printHTML(final String html) {
            runOnUiThread(() -> {
                if (printerConnected && woyouService != null) {
                    printWithSunmi(html);
                } else {
                    bindSunmiService();
                    webView.postDelayed(() -> {
                        if (printerConnected && woyouService != null) {
                            printWithSunmi(html);
                        } else {
                            webView.evaluateJavascript(
                                "alert('Imprimante Sunmi non disponible.');", null);
                        }
                    }, 1500);
                }
            });
        }

        @JavascriptInterface
        public boolean isPrinterReady() {
            return printerConnected && woyouService != null;
        }
    }

    private void printWithSunmi(String html) {
        try {
            woyouService.printerInit(null);

            String text = html
                .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<div[^>]*>", "\n")
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

            String[] lines = text.split("\n");
            boolean headerDone = false;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (!headerDone && (line.toUpperCase().contains("LOTATO"))) {
                    woyouService.setAlignment(1, null);
                    woyouService.setFontSize(32, null);
                    woyouService.printTextWithFont(line + "\n", null, 32, null);
                    woyouService.setFontSize(24, null);
                    headerDone = true;
                } else if (line.toUpperCase().contains("TOTAL")) {
                    woyouService.setAlignment(0, null);
                    woyouService.setFontSize(28, null);
                    woyouService.printText(line + "\n", null);
                    woyouService.setFontSize(24, null);
                } else if (line.matches("[-=]{3,}")) {
                    woyouService.printText("--------------------------------\n", null);
                } else {
                    woyouService.setAlignment(0, null);
                    woyouService.setFontSize(24, null);
                    woyouService.printText(line + "\n", null);
                }
            }

            woyouService.lineWrap(3, null);
            woyouService.cutPaper(null);

            Log.d(TAG, "✅ Impression Sunmi terminée");

        } catch (RemoteException e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
            final String msg = e.getMessage() != null ? e.getMessage().replace("'", "") : "Erè enkoni";
            runOnUiThread(() ->
                webView.evaluateJavascript("alert('Erè enpresyon: " + msg + "');", null)
            );
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
        try { unbindService(serviceConnection); } catch (Exception ignored) {}
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
