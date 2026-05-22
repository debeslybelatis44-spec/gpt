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

    private static final String TAG = "LOTATO";
    private WebView webView;
    private IWoyouService woyouService;
    private boolean printerConnected = false;
    private String pendingPrintHTML = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            printerConnected = true;
            Log.d(TAG, "✅ Sunmi connecté: " + name.flattenToString());

            if (pendingPrintHTML != null) {
                final String html = pendingPrintHTML;
                pendingPrintHTML = null;
                runOnUiThread(() -> printWithSunmi(html));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            woyouService = null;
            printerConnected = false;
            Log.w(TAG, "Sunmi déconnecté");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Essayer de lier le service Sunmi avec ComponentName explicite
        bindSunmiPrinter();

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

        webView.addJavascriptInterface(new SunmiBridge(), "AndroidPrint");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://lotato1.onrender.com/agent1.html");
    }

    private void bindSunmiPrinter() {
        // Méthode 1 — ComponentName explicite (la plus fiable sur Framework 2.0.0)
        try {
            Intent intent1 = new Intent();
            intent1.setComponent(new ComponentName(
                "woyou.aidlservice.jiuiv5",
                "woyou.aidlservice.jiuiv5.InnerPrinterService"
            ));
            boolean b1 = bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Méthode 1 (ComponentName explicite): " + b1);
            if (b1) return;
        } catch (Exception e) {
            Log.e(TAG, "M1 erreur: " + e.getMessage());
        }

        // Méthode 2 — Action + Package
        try {
            Intent intent2 = new Intent("woyou.aidlservice.jiuiv5.IWoyouService");
            intent2.setPackage("woyou.aidlservice.jiuiv5");
            boolean b2 = bindService(intent2, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Méthode 2 (Action+Package): " + b2);
            if (b2) return;
        } catch (Exception e) {
            Log.e(TAG, "M2 erreur: " + e.getMessage());
        }

        // Méthode 3 — com.sunmi.innerprinter (Framework 2.x)
        try {
            Intent intent3 = new Intent();
            intent3.setComponent(new ComponentName(
                "com.sunmi.innerprinter",
                "com.sunmi.innerprinter.InnerPrinterService"
            ));
            boolean b3 = bindService(intent3, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Méthode 3 (sunmi.innerprinter): " + b3);
            if (b3) return;
        } catch (Exception e) {
            Log.e(TAG, "M3 erreur: " + e.getMessage());
        }

        // Méthode 4 — Action seule
        try {
            Intent intent4 = new Intent("com.sunmi.innerprinter.ISunmiPrinterService");
            intent4.setPackage("com.sunmi.innerprinter");
            boolean b4 = bindService(intent4, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Méthode 4 (innerprinter action): " + b4);
        } catch (Exception e) {
            Log.e(TAG, "M4 erreur: " + e.getMessage());
        }
    }

    public class SunmiBridge {

        @JavascriptInterface
        public void printHTML(final String html) {
            Log.d(TAG, "printHTML appelé - connecté=" + printerConnected);
            runOnUiThread(() -> {
                if (printerConnected && woyouService != null) {
                    printWithSunmi(html);
                } else {
                    pendingPrintHTML = html;
                    bindSunmiPrinter();
                    webView.postDelayed(() -> {
                        if (pendingPrintHTML != null) {
                            if (printerConnected && woyouService != null) {
                                final String h = pendingPrintHTML;
                                pendingPrintHTML = null;
                                printWithSunmi(h);
                            } else {
                                pendingPrintHTML = null;
                                webView.evaluateJavascript(
                                    "alert('Erè: Sèvis Sunmi pa jwenn. Rekomanse app la.');",
                                    null
                                );
                            }
                        }
                    }, 4000);
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
                .replaceAll("</div>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.toUpperCase().contains("LOTATO")) {
                    woyouService.setAlignment(1, null);
                    woyouService.setFontSize(28, null);
                    woyouService.printText(line + "\n", null);
                    woyouService.setAlignment(0, null);
                    woyouService.setFontSize(24, null);
                } else if (line.toUpperCase().startsWith("TOTAL")) {
                    woyouService.setFontSize(26, null);
                    woyouService.printText(line + "\n", null);
                    woyouService.setFontSize(24, null);
                } else if (line.matches("[-=]{3,}")) {
                    woyouService.printText("--------------------------------\n", null);
                } else {
                    woyouService.setFontSize(24, null);
                    woyouService.printText(line + "\n", null);
                }
            }

            woyouService.lineWrap(4, null);
            woyouService.cutPaper(null);
            Log.d(TAG, "✅ Impression OK");

        } catch (RemoteException e) {
            Log.e(TAG, "Erreur impression: " + e.getMessage());
            final String msg = e.getMessage() != null
                ? e.getMessage().replace("'", "")
                : "Erè enkoni";
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
