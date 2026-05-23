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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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

    // ✅ Construire commandes ESC/POS en bytes — évite tout problème d'encodage
    private byte[] buildEscPosData(String html) {
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

        // Remplacer caractères créoles non-ASCII par equivalents ASCII
        text = text
            .replace("è", "e").replace("é", "e").replace("ê", "e")
            .replace("à", "a").replace("â", "a")
            .replace("ò", "o").replace("ô", "o")
            .replace("ù", "u").replace("û", "u")
            .replace("î", "i").replace("ï", "i")
            .replace("ç", "c")
            .replace("È", "E").replace("É", "E")
            .replace("À", "A").replace("Â", "A")
            .replace("'", "'").replace("\u2019", "'")
            .replace("\u00AB", "\"").replace("\u00BB", "\"");

        List<Byte> bytes = new ArrayList<>();

        // ESC @ — init imprimante
        addBytes(bytes, new byte[]{0x1B, 0x40});

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();

            // Ligne vide
            if (line.isEmpty()) {
                addBytes(bytes, "\n".getBytes());
                continue;
            }

            // Séparateur
            if (line.matches("[-=]{3,}")) {
                addBytes(bytes, "--------------------------------\n".getBytes());
                continue;
            }

            // Ligne normale
            try {
                addBytes(bytes, (line + "\n").getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                addBytes(bytes, (line + "\n").getBytes());
            }
        }

        // Avancer papier — ESC d n (4 lignes)
        addBytes(bytes, new byte[]{0x1B, 0x64, 0x04});

        // Couper papier — GS V (coupe partielle)
        addBytes(bytes, new byte[]{0x1D, 0x56, 0x01});

        // Convertir List<Byte> → byte[]
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    private void addBytes(List<Byte> list, byte[] data) {
        for (byte b : data) list.add(b);
    }

    private void printWithSunmi(String html) {
        try {
            woyouService.printerInit(null);

            // ✅ Envoyer en RAW bytes ESC/POS — contourne tous les problèmes d'encodage
            byte[] data = buildEscPosData(html);
            woyouService.sendRAWData(data, null);

            Log.d(TAG, "✅ Impression RAW envoyée (" + data.length + " bytes)");

        } catch (RemoteException e) {
            Log.e(TAG, "Erreur impression: " + e.getMessage());
            final String msg = e.getMessage() != null
                ? e.getMessage().replace("'", "")
                : "Erè enkoni";
            runOnUiThread(() ->
                webView.evaluateJavascript("alert('Ere: " + msg + "');", null)
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
