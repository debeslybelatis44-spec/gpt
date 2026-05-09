package com.sunmi.peripheral.printer;

import com.sunmi.peripheral.printer.InnerPrinterCallback;

interface SunmiPrinterService {
    void printerInit(in InnerPrinterCallback callback);
    void setAlignment(in int alignment, in InnerPrinterCallback callback);
    void printText(in String text, in InnerPrinterCallback callback);
    void printBitmap(in android.graphics.Bitmap bitmap, in InnerPrinterCallback callback);
    void printBarcode(in String data, in int type, in int width, in int height, in InnerPrinterCallback callback);
    void printQRCode(in String data, in int size, in InnerPrinterCallback callback);
    void lineWrap(in int lines, in InnerPrinterCallback callback);
    void cutPaper(in InnerPrinterCallback callback);
    int getPrinterState();
    void setFontSize(in float size, in InnerPrinterCallback callback);
    void setFontStyle(in int style, in InnerPrinterCallback callback);
}
