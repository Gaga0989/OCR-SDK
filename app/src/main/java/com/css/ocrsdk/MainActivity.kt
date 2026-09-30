package com.css.ocrsdk

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.css.ocr_scanner.OcrScannerSdk
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File

class MainActivity : AppCompatActivity() {
    private val tvMessage by lazy { findViewById<TextView>(R.id.tvMessage) }
    private val tvFullName by lazy { findViewById<TextView>(R.id.tvFullName) }
    private val tvIdentityNum by lazy { findViewById<TextView>(R.id.tvIdentityNum) }
    private val tvType by lazy { findViewById<TextView>(R.id.tvType) }
    private val tvFrameSetting by lazy { findViewById<TextView>(R.id.tvFrameSetting) }
    private val imgImage by lazy { findViewById<ImageView>(R.id.imgImage) }
    private val tvAgain by lazy { findViewById<TextView>(R.id.tvAgain) }
    private var left1= 0
    private var left2= 0
    private var top1= 0
    private var top2= 0
    private var right1= 0
    private var right2= 0
    private var bottom1= 0
    private var bottom2= 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // R = android 11
            //tvDeviceInfo.text = "Below 11. Android SDK " + Build.VERSION.SDK_INT.toString()
            requestForPermissions()
        } else {
            //tvDeviceInfo.text = "Android SDK " + Build.VERSION.SDK_INT.toString()
            requestForPermissionsHighThan() //Android 11
        }

        tvAgain.setOnClickListener(View.OnClickListener {
            toSDk()
        })
    }

    private fun toSDk() {
        OcrScannerSdk.startOcr(this, launcher = ocrLauncher,
        left1 = left1, left2 = left2, top1 = top1, top2 = top2, right1 = right1, right2 = right2, bottom1 = bottom1, bottom2 = bottom2)
    }
    private val ocrLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val code = data.getStringExtra("code")
                val message = data.getStringExtra("message")
                val imagePath = data.getStringExtra("imagePath")
                val identityNum = data.getStringExtra("identityNum")
                val fullName = data.getStringExtra("fullName")
                val type = data.getStringExtra("type")

                left1 = data.getIntExtra("left1", 0)
                left2 = data.getIntExtra("left2",0)
                top1 = data.getIntExtra("top1",0)
                top2 = data.getIntExtra("top2",0)
                right1 = data.getIntExtra("right1",0)
                right2 = data.getIntExtra("right2",0)
                bottom1 = data.getIntExtra("bottom1",0)
                bottom2 = data.getIntExtra("bottom2",0)

                // TODO: 处理 OCR 结果
                tvMessage.text = message
                if (code == "0") {
                    tvFullName.text = fullName
                    tvIdentityNum.text = identityNum
                    tvType.text = type
                    tvFrameSetting.text = left1.toString() + " + " + left2.toString() + " + " +top1.toString() + " + " +
                            top2.toString() + " + " +right1.toString()+ " + " + right2.toString() + " + " +bottom1.toString() + " + " +bottom2.toString()
                    try {
                        imgImage.setImageURI(Uri.fromFile(File(imagePath)))
                    } catch(e: Exception) {

                    }
                    imgImage.visibility = View.VISIBLE
                }
            }
        }

    private fun requestForPermissions() {
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        toSDk()
                    } else if (!multiplePermissionsReport.areAllPermissionsGranted()) {
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                            val snackbar = Snackbar.make(
                                findViewById(android.R.id.content),
                                "Please allow access to photos and media on your device.",
                                Snackbar.LENGTH_LONG
                            )
                            snackbar.setActionTextColor(Color.WHITE).setAction(
                                "Settings"
                            ) {
                                val intent = Intent()
                                intent.action =
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts(
                                    "package",
                                    packageName, null
                                )
                                intent.data = uri
                                startActivity(intent)
                            }
                            snackbar.show()
                        }
                    } else {
                        requestForPermissions()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }

    private fun requestForPermissionsHighThan() {
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        toSDk()
                    } else if (!multiplePermissionsReport.areAllPermissionsGranted()) {
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                            val snackbar = Snackbar.make(
                                findViewById(android.R.id.content),
                                "Please allow access to photos and media on your device.",
                                Snackbar.LENGTH_LONG
                            )
                            snackbar.setActionTextColor(Color.WHITE).setAction(
                                "Settings"
                            ) {
                                val intent = Intent()
                                intent.action =
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts(
                                    "package",
                                    packageName, null
                                )
                                intent.data = uri
                                startActivity(intent)
                            }
                            snackbar.show()
                        }
                    } else {
                        requestForPermissionsHighThan()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }
}