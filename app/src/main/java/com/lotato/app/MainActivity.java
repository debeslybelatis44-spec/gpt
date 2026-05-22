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

    // ✅ Les 3 combinaisons possibles sur Sunmi Framework 2.0.0
    private static final String[][] SUNMI_SERVICES = {
        {"woyou.aidlservice.jiuiv5", "woyou.aidlservice.jiuiv5.IWoyouService"},
        {"com.sunmi.innerprinter",   "com.sunmi.innerprinter.ISunmiPrinterService"},
        {"woyou.aidlservice.jiuiv5", "woyou.aidlservice.jiuiv5.IWoyouService"}
    };
    private int serviceAttempt = 0;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            printerConnected = true;
            Log.d(TAG, "✅ Sunmi connecté via: " + name.getPackageName());

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

        tryBindSunmiService(0);

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

    // ✅ Essayer chaque combinaison de service jusqu'à trouver la bonne
    private void tryBindSunmiService(int attempt) {
        if (attempt >= SUNMI_SERVICES.length) {
            Log.e(TAG, "Aucun service Sunmi trouvé après " + attempt + " tentatives");
            return;
        }

        String pkg    = SUNMI_SERVICES[attempt][0];
        String action = SUNMI_SERVICES[attempt][1];

        try {
            Intent intent = new Intent();
            intent.setPackage(pkg);
            intent.setAction(action);
            boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Tentative " + attempt + " [" + pkg + "]: " + bound);

            if (!bound) {
                // Essayer la combinaison suivante
                tryBindSunmiService(attempt + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur tentative " + attempt + ": " + e.getMessage());
            tryBindSunmiService(attempt + 1);
        }
    }

    public class SunmiBridge {

        @JavascriptInterface
        public void printHTML(final String html) {
            Log.d(TAG, "printHTML - connecté=" + printerConnected);
            runOnUiThread(() -> {
                if (printerConnected && woyouService != null) {
                    printWithSunmi(html);
                } else {
                    pendingPrintHTML = html;
                    tryBindSunmiService(0);
                    // Attendre 4 secondes max
                    webView.postDelayed(() -> {
                        if (pendingPrintHTML != null) {
                            // Dernière chance - essayer impression directe
                            if (printerConnected && woyouService != null) {
                                final String h = pendingPrintHTML;
                                pendingPrintHTML = null;
                                printWithSunmi(h);
                            } else {
                                pendingPrintHTML = null;
                                webView.evaluateJavascript(
                                    "alert('Imprimante pa disponib. Tcheke ke Sunmi aktif epi reesye.');",
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

        // ✅ Méthode de diagnostic - retourne l'état au JS
        @JavascriptInterface
        public String getDiagnostic() {
            return "connected=" + printerConnected + " service=" + (woyouService != null);
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
            final String msg = e.getMessage() != null ? e.getMessage().replace("'","") : "Erè enkoni";
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
