package com.css.ocr_scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object OcrScannerSdk {

    const val REQUEST_CODE = 9091
    const val EXTRA_OCR_RESULT = "extra_ocr_result"

    fun startOcr(
        activity: Activity,
        left1: Int? = null,
        left2: Int? = null,
        top1: Int? = null,
        top2: Int? = null,
        right1: Int? = null,
        right2: Int? = null,
        bottom1:Int? = null,
        bottom2:Int? = null,
        launcher: ActivityResultLauncher<Intent>
    ) {
        /*require(!secretCode.isNullOrBlank()) {
            "OcrScannerSdk: secretCode is required"
        }*/
        var sfleft1 = left1 ?: 0
        var sfleft2 = left2 ?: 0
        var sftop1 = top1 ?: 0
        var sftop2 = top2 ?: 0
        var sfright1 = right1 ?: 0
        var sfright2 = right2 ?: 0
        var sfbottom1 = bottom1 ?: 0
        var sfbottom2 = bottom2 ?: 0

        val totalSize = sfleft1 + sfleft2 + sftop1 + sftop2 + sfright1 + sfright2 + sfbottom1 + sfbottom2
        if (totalSize == 0) {
            sfleft1 = 6
            sfleft2 = 5
            sftop1 = 3 // 切3格子
            sftop2 = 1 // 以画面中心开始计算拿1个格子
            sfright1 = 6
            sfright2 = 5
            sfbottom1 = 3
            sfbottom2 = 1
        }
        CaptureActivity.left1 = sfleft1
        CaptureActivity.left2 = sfleft2
        CaptureActivity.top1 = sftop1
        CaptureActivity.top2 = sftop2
        CaptureActivity.right1 = sfright1
        CaptureActivity.right2 = sfright2
        CaptureActivity.bottom1 = sfbottom1
        CaptureActivity.bottom2 = sfbottom2
        Log.e("OCR_SDK", "start() called")
        var strCode = "0"
        var strMessage = ""
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // R = android 11
            /*if (!hasStoragePermissionSDK_Low(activity)) {
                strCode = "99"
                strMessage = "Please allow permission: storage"
            }*/
        } else {
            if (!hasCameraPermission(activity)) {
                strCode = "99"
                strMessage = "Please allow permission: camera"
            }
        }
        val intent = Intent(activity, CaptureActivity::class.java)
        intent
            .putExtra("code", strCode)
            .putExtra("message", strMessage)
        //activity.startActivity(intent)
        launcher.launch(intent)
    }

    private fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun hasStoragePermissionSDK_Low(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!read || !write) {
            return false
        }
        return true
    }
}

