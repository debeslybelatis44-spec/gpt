package com.lotato.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import woyou.aidlservice.jiuiv5.IWoyouService;

import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {

    private static final String TAG = "LOTATO";
    private WebView webView;
    private IWoyouService woyouService;
    private boolean printerConnected = false;
    private String pendingPrintHTML = null;
    private StringBuilder logBuffer = new StringBuilder();
    private TextView logView;
    private ScrollView logScroll;
    private boolean logVisible = false;
    private Handler handler = new Handler();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            printerConnected = true;
            addLog("✅ CONNECTE");
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
            addLog("❌ DECONNECTE");
            handler.postDelayed(() -> bindSunmiPrinter(), 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        logScroll = new ScrollView(this);
        logScroll.setBackgroundColor(0xDD000000);
        logView = new TextView(this);
        logView.setTextColor(0xFF00FF00);
        logView.setTextSize(10);
        logView.setPadding(10, 10, 10, 10);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logScroll.addView(logView);
        FrameLayout.LayoutParams logParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 400);
        logParams.gravity = Gravity.BOTTOM;
        logParams.bottomMargin = 60;
        logScroll.setVisibility(View.GONE);
        root.addView(logScroll, logParams);

        Button debugBtn = new Button(this);
        debugBtn.setText("LOG");
        debugBtn.setTextSize(9);
        debugBtn.setBackgroundColor(0xAA333333);
        debugBtn.setTextColor(0xFFFFFF00);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(120, 60);
        btnParams.gravity = Gravity.BOTTOM | Gravity.END;
        root.addView(debugBtn, btnParams);
        debugBtn.setOnClickListener(v -> {
            logVisible = !logVisible;
            logScroll.setVisibility(logVisible ? View.VISIBLE : View.GONE);
            if (logVisible) logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });

        setContentView(root);

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

        addLog("App demarre");
        bindSunmiPrinter();
    }

    private void addLog(String msg) {
        Log.d(TAG, msg);
        runOnUiThread(() -> {
            logBuffer.append(msg).append("\n");
            if (logView != null) {
                logView.setText(logBuffer.toString());
                logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void bindSunmiPrinter() {
        addLog("--- Liaison ---");
        try {
            Intent i = new Intent("woyou.aidlservice.jiuiv5.IWoyouService");
            i.setPackage("woyou.aidlservice.jiuiv5");
            boolean b = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            addLog("M2: " + b);
            if (b) return;
        } catch (Exception e) { addLog("M2 ERR: " + e.getMessage()); }

        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(
                "woyou.aidlservice.jiuiv5",
                "woyou.aidlservice.jiuiv5.InnerPrinterService"));
            boolean b = bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
            addLog("M1: " + b);
        } catch (Exception e) { addLog("M1 ERR: " + e.getMessage()); }
    }

    public class SunmiBridge {
        @JavascriptInterface
        public void printHTML(final String html) {
            addLog("printHTML len=" + html.length());
            runOnUiThread(() -> {
                if (printerConnected && woyouService != null) {
                    printWithSunmi(html);
                } else {
                    pendingPrintHTML = html;
                    bindSunmiPrinter();
                    handler.postDelayed(() -> {
                        if (pendingPrintHTML != null) {
                            if (printerConnected && woyouService != null) {
                                final String h = pendingPrintHTML;
                                pendingPrintHTML = null;
                                printWithSunmi(h);
                            } else {
                                pendingPrintHTML = null;
                                webView.evaluateJavascript("alert('Sunmi pa jwenn.');", null);
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

    private String cleanText(String input) {
        if (input == null) return "";
        return input
            .replace("\u00e8","e").replace("\u00e9","e").replace("\u00ea","e")
            .replace("\u00e0","a").replace("\u00e2","a")
            .replace("\u00f2","o").replace("\u00f4","o")
            .replace("\u00f9","u").replace("\u00fb","u")
            .replace("\u00ee","i").replace("\u00ef","i")
            .replace("\u00e7","c")
            .replace("\u00c8","E").replace("\u00c9","E")
            .replace("\u00c0","A").replace("\u00c2","A")
            .replace("\u2019","'").replace("\u2018","'")
            .replace("\u201c","\"").replace("\u201d","\"")
            .replace("\u00ab","\"").replace("\u00bb","\"")
            .replace("\u2013","-").replace("\u2014","-")
            .replaceAll("[^\\x00-\\x7F]","?");
    }

    private void printWithSunmi(final String html) {
        addLog("--- Impression ---");
        new Thread(() -> {
            try {
                String text = html
                    .replaceAll("(?s)<style[^>]*>.*?</style>","")
                    .replaceAll("<br\\s*/?>","\n")
                    .replaceAll("</div>","\n")
                    .replaceAll("</p>","\n")
                    .replaceAll("<[^>]+>","")
                    .replaceAll("&nbsp;"," ")
                    .replaceAll("&amp;","&")
                    .replaceAll("\n{3,}","\n\n")
                    .trim();
                text = cleanText(text);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                // ESC @ reset
                bos.write(new byte[]{0x1B, 0x40});

                for (String line : text.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        bos.write('\n');
                        continue;
                    }
                    if (line.matches("[-=]{3,}")) line = "--------------------------------";
                    bos.write(line.getBytes("ASCII"));
                    bos.write('\n');
                }

                // Feed 8 lignes
                bos.write(new byte[]{0x1B, 0x64, 0x08});

                byte[] data = bos.toByteArray();
                addLog("Bytes: " + data.length);

                // ✅ Essayer printRawData d'abord
                try {
                    woyouService.printRawData(data, null);
                    addLog("✅ printRawData OK");
                } catch (Exception e1) {
                    addLog("printRawData ERR: " + e1.getMessage());
                    // Fallback: sendRAWData
                    try {
                        woyouService.sendRAWData(data, null);
                        addLog("✅ sendRAWData OK");
                    } catch (Exception e2) {
                        addLog("sendRAWData ERR: " + e2.getMessage());
                        // Dernier fallback: printText ligne par ligne
                        woyouService.printerInit(null);
                        for (String line : text.split("\n")) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                woyouService.printText(line + "\n", null);
                            }
                        }
                        woyouService.lineWrap(8, null);
                        addLog("✅ printText fallback OK");
                    }
                }

            } catch (Exception e) {
                addLog("❌ " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try { unbindService(serviceConnection); } catch (Exception ignored) {}
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
