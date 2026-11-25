package com.codeframe78.twentyfourseven.player.companion

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

sealed interface QrScanResult {
    data class Scanned(val text: String) : QrScanResult
    data object Cancelled : QrScanResult
    data object Unavailable : QrScanResult
}

/**
 * Scans the TV's QR code with Google Play services' on-device code scanner. Google's scanner screen holds the camera,
 * so this app is never granted camera access and only receives the decoded text.
 */
class CompanionQrScanner(private val context: Context) {
    fun scan(onResult: (QrScanResult) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val text = barcode.rawValue
                onResult(if (text.isNullOrEmpty()) QrScanResult.Cancelled else QrScanResult.Scanned(text))
            }
            .addOnCanceledListener { onResult(QrScanResult.Cancelled) }
            .addOnFailureListener { onResult(QrScanResult.Unavailable) }
    }
}
