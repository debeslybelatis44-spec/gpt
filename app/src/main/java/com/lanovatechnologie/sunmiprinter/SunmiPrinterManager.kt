package com.lanovatechnologie.sunmiprinter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.sunmi.peripheral.printer.WoyouConsts

class SunmiPrinterManager(private val context: Context) {
    private var sunmiPrinterService: SunmiPrinterService? = null
    private var isConnected = false

    init {
        try {
            // Initialiser le service Sunmi
            sunmiPrinterService = SunmiPrinterService(context)
            sunmiPrinterService?.printerInit(object : InnerPrinterCallback() {
                override fun onConnected(service: SunmiPrinterService) {
                    isConnected = true
                    Log.d("SunmiPrinter", "Imprimante connectée")
                }

                override fun onDisconnected() {
                    isConnected = false
                    Log.d("SunmiPrinter", "Imprimante déconnectée")
                }
            })
        } catch (e: Exception) {
            Log.e("SunmiPrinter", "Erreur lors de l'initialisation: ${e.message}")
        }
    }

    fun printText(text: String) {
        if (!isConnected) {
            Log.e("SunmiPrinter", "Imprimante non connectée")
            return
        }
        try {
            sunmiPrinterService?.printText(text, object : InnerPrinterCallback() {
                override fun onReturnString(result: String) {
                    Log.d("SunmiPrinter", "Texte imprimé: $result")
                }
            })
            sunmiPrinterService?.lineWrap(3, null)
        } catch (e: Exception) {
            Log.e("SunmiPrinter", "Erreur lors de l'impression du texte: ${e.message}")
        }
    }

    fun printImage(base64Image: String) {
        if (!isConnected) {
            Log.e("SunmiPrinter", "Imprimante non connectée")
            return
        }
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.let {
                sunmiPrinterService?.printBitmap(it, object : InnerPrinterCallback() {
                    override fun onReturnString(result: String) {
                        Log.d("SunmiPrinter", "Image imprimée: $result")
                    }
                })
                sunmiPrinterService?.lineWrap(3, null)
            }
        } catch (e: Exception) {
            Log.e("SunmiPrinter", "Erreur lors de l'impression de l'image: ${e.message}")
        }
    }

    fun printBarcode(barcodeData: String) {
        if (!isConnected) {
            Log.e("SunmiPrinter", "Imprimante non connectée")
            return
        }
        try {
            sunmiPrinterService?.printBarcode(
                barcodeData,
                WoyouConsts.BARCODE_TYPE_CODE128,
                100,
                2,
                object : InnerPrinterCallback() {
                    override fun onReturnString(result: String) {
                        Log.d("SunmiPrinter", "Code-barres imprimé: $result")
                    }
                }
            )
            sunmiPrinterService?.lineWrap(3, null)
        } catch (e: Exception) {
            Log.e("SunmiPrinter", "Erreur lors de l'impression du code-barres: ${e.message}")
        }
    }

    fun printQRCode(qrData: String) {
        if (!isConnected) {
            Log.e("SunmiPrinter", "Imprimante non connectée")
            return
        }
        try {
            sunmiPrinterService?.printQRCode(
                qrData,
                200,
                object : InnerPrinterCallback() {
                    override fun onReturnString(result: String) {
                        Log.d("SunmiPrinter", "QR code imprimé: $result")
                    }
                }
            )
            sunmiPrinterService?.lineWrap(3, null)
        } catch (e: Exception) {
            Log.e("SunmiPrinter", "Erreur lors de l'impression du QR code: ${e.message}")
        }
    }

    fun isPrinterConnected(): Boolean {
        return isConnected
    }
}
