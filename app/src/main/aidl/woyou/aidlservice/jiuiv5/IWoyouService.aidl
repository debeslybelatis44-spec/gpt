// IWoyouService.aidl
package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;

interface IWoyouService {
    void printerInit(in ICallback callback);
    void printerSelfChecking(in ICallback callback);
    String getPrinterSerialNo();
    String getPrinterModal();
    String getPrinterVersion();
    int updateFirmware();
    int getFirmwareStatus();
    String getServiceVersion();
    void setPrinterStyle(in int key, in int value, in ICallback callback);
    void setAlignment(in int alignment, in ICallback callback);
    void setFontName(in String typeface, in ICallback callback);
    void setFontSize(in float fontsize, in ICallback callback);
    void setBold(in boolean enable, in ICallback callback);
    void printText(in String text, in ICallback callback);
    void printTextWithFont(in String text, in String typeface, in float fontsize, in ICallback callback);
    void printColumnsText(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlignArr, in ICallback callback);
    void printBitmap(in Bitmap bitmap, in ICallback callback);
    void printBarCode(in String data, in int symbology, in int height, in int width, in int textposition, in ICallback callback);
    void printQRCode(in String data, in int modulesize, in int errorlevel, in ICallback callback);
    void printRawData(in byte[] rawPrintData, in ICallback callback);
    void lineWrap(in int lines, in ICallback callback);
    void cutPaper(in ICallback callback);
    int getPrinterStatus();
    void sendRAWData(in byte[] bytes, in ICallback callback);
    void openDrawer();
    void closeDrawer();
    boolean isDrawerOpen();
    void printBitmapCustom(in Bitmap bitmap, in int type, in ICallback callback);
}

