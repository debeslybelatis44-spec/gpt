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

    // File d'attente si impression demandée avant connexion
    private String pendingPrintHTML = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            printerConnected = true;
            Log.d(TAG, "✅ Service Sunmi connecté");

            // S'il y avait une impression en attente, l'exécuter maintenant
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
            Log.d(TAG, "❌ Service Sunmi déconnecté - tentative reconnexion");
            // Reconnexion automatique
            bindSunmiService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lier Sunmi dès le démarrage
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
        try {
            Intent intent = new Intent();
            intent.setPackage("woyou.aidlservice.jiuiv5");
            intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
            boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Liaison Sunmi: " + bound);
        } catch (Exception e) {
            Log.e(TAG, "Erreur liaison Sunmi: " + e.getMessage());
        }
    }

    public class SunmiBridge {

        @JavascriptInterface
        public void printHTML(final String html) {
            Log.d(TAG, "printHTML appelé - printerConnected=" + printerConnected);
            runOnUiThread(() -> {
                if (printerConnected && woyouService != null) {
                    // Service prêt, imprimer directement
                    printWithSunmi(html);
                } else {
                    // Mettre en file d'attente et reconnecter
                    Log.w(TAG, "Service pas prêt - mise en attente");
                    pendingPrintHTML = html;
                    bindSunmiService();
                    // Timeout 3 secondes
                    webView.postDelayed(() -> {
                        if (pendingPrintHTML != null) {
                            pendingPrintHTML = null;
                            webView.evaluateJavascript(
                                "alert('Imprimante Sunmi pa disponib. Reesye.');",
                                null
                            );
                        }
                    }, 3000);
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
            // Initialiser l'imprimante
            woyouService.printerInit(null);

            // Nettoyer le HTML pour extraire le texte
            String text = html
                .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</div>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

            String[] lines = text.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Header - nom loterie centré en grand
                if (line.toUpperCase().contains("LOTATO")) {
                    woyouService.setAlignment(1, null);
                    woyouService.setFontSize(28, null);
                    woyouService.printText(line + "\n", null);
                    woyouService.setAlignment(0, null);
                    woyouService.setFontSize(24, null);
                }
                // Ligne TOTAL en gras
                else if (line.toUpperCase().startsWith("TOTAL")) {
                    woyouService.setFontSize(26, null);
                    woyouService.printText(line + "\n", null);
                    woyouService.setFontSize(24, null);
                }
                // Séparateur
                else if (line.matches("[-=]{3,}")) {
                    woyouService.printText("--------------------------------\n", null);
                }
                // Ligne normale
                else {
                    woyouService.setAlignment(0, null);
                    woyouService.setFontSize(24, null);
                    woyouService.printText(line + "\n", null);
                }
            }

            // Avancer le papier et couper
            woyouService.lineWrap(4, null);
            woyouService.cutPaper(null);

            Log.d(TAG, "✅ Impression terminée");

        } catch (RemoteException e) {
            Log.e(TAG, "Erreur impression: " + e.getMessage());
            final String msg = (e.getMessage() != null)
                ? e.getMessage().replace("'", "").replace("\"", "")
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
        try {
            unbindService(serviceConnection);
        } catch (Exception ignored) {}
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
