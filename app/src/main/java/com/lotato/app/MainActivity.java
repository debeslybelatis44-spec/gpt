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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(
                "woyou.aidlservice.jiuiv5",
                "woyou.aidlservice.jiuiv5.InnerPrinterService"
            ));
            boolean b = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Bind M1: " + b);
            if (b) return;
        } catch (Exception e) { Log.e(TAG, "M1: " + e.getMessage()); }

        try {
            Intent i = new Intent("woyou.aidlservice.jiuiv5.IWoyouService");
            i.setPackage("woyou.aidlservice.jiuiv5");
            boolean b = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Bind M2: " + b);
            if (b) return;
        } catch (Exception e) { Log.e(TAG, "M2: " + e.getMessage()); }

        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(
                "com.sunmi.innerprinter",
                "com.sunmi.innerprinter.InnerPrinterService"
            ));
            boolean b = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Bind M3: " + b);
        } catch (Exception e) { Log.e(TAG, "M3: " + e.getMessage()); }
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
                                    "alert('Sèvis Sunmi pa jwenn.');", null);
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

    // ✅ Nettoyer TOUS les caractères non-ASCII avant printText
    private String cleanForPrinter(String input) {
        if (input == null) return "";
        return input
            // Créole / Français
            .replace("\u00e8","e").replace("\u00e9","e").replace("\u00ea","e").replace("\u00eb","e")
            .replace("\u00e0","a").replace("\u00e2","a").replace("\u00e4","a")
            .replace("\u00f2","o").replace("\u00f4","o").replace("\u00f6","o")
            .replace("\u00f9","u").replace("\u00fb","u").replace("\u00fc","u")
            .replace("\u00ee","i").replace("\u00ef","i")
            .replace("\u00e7","c")
            .replace("\u00c8","E").replace("\u00c9","E").replace("\u00ca","E")
            .replace("\u00c0","A").replace("\u00c2","A")
            .replace("\u00d2","O").replace("\u00d4","O")
            .replace("\u00d9","U").replace("\u00db","U")
            .replace("\u00ce","I")
            .replace("\u00c7","C")
            // Guillemets et apostrophes
            .replace("\u2019","'").replace("\u2018","'")
            .replace("\u201c","\"").replace("\u201d","\"")
            .replace("\u00ab","\"").replace("\u00bb","\"")
            // Tirets spéciaux
            .replace("\u2013","-").replace("\u2014","-")
            // Supprimer tout caractère restant > 127
            .replaceAll("[^\\x00-\\x7F]", "?");
    }

    private void printWithSunmi(String html) {
        try {
            // Nettoyer HTML → texte pur
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

            // ✅ Nettoyer TOUS les accents AVANT d'envoyer à printText
            text = cleanForPrinter(text);

            // Init imprimante
            woyouService.printerInit(null);

            // Imprimer ligne par ligne avec try/catch individuel
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.matches("[-=]{3,}")) {
                    line = "--------------------------------";
                }

                try {
                    woyouService.printText(line + "\n", null);
                } catch (RemoteException re) {
                    Log.e(TAG, "Erreur ligne [" + line + "]: " + re.getMessage());
                    // Continuer malgré l'erreur sur une ligne
                }
            }

            // Avancer et couper
            woyouService.lineWrap(4, null);
            woyouService.cutPaper(null);

            Log.d(TAG, "✅ Impression terminee");

        } catch (Exception e) {
            Log.e(TAG, "Erreur globale: " + e.getMessage());
            final String msg = e.getMessage() != null
                ? e.getMessage().replace("'","")
                : "unknown";
            runOnUiThread(() ->
                webView.evaluateJavascript("alert('Ere impression: " + msg + "');", null)
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
