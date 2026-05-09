package com.lanovatechnologie.sunmiprinter

import android.content.Context
import android.webkit.JavascriptInterface

class WebAppInterface(private val context: Context) {
    private val printerManager = SunmiPrinterManager(context)

    @JavascriptInterface
    fun printTicket(ticketData: String) {
        printerManager.printText(ticketData)
    }

    @JavascriptInterface
    fun printImage(base64Image: String) {
        printerManager.printImage(base64Image)
    }

    @JavascriptInterface
    fun printBarcode(barcodeData: String) {
        printerManager.printBarcode(barcodeData)
    }

    @JavascriptInterface
    fun printQRCode(qrData: String) {
        printerManager.printQRCode(qrData)
    }
}
