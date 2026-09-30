package com.css.ocr_scanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

class FinishActivity : AppCompatActivity() {
    private val tvMessage by lazy { findViewById<TextView>(R.id.tvMessage) }
    private val tvBack by lazy { findViewById<TextView>(R.id.tvBack) }
    private val tvFullName by lazy { findViewById<TextView>(R.id.tvFullName) }
    private val tvIdentityNum by lazy { findViewById<TextView>(R.id.tvIdentityNum) }
    private val tvType by lazy { findViewById<TextView>(R.id.tvType) }
    private val imgImage by lazy { findViewById<ImageView>(R.id.imgImage) }
    private var code:String? = null
    private var message:String? = null
    private var imagePath:String = ""
    private var identityNum:String? = null
    private var fullName:String? = null
    private var type:String? = null
    private var btBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)

        val bundle = intent.extras
        if (bundle != null) {
            code = bundle.getString("code") // 0 success
            message = bundle.getString("message")
            imagePath = bundle.getString("imagePath").toString()
            identityNum = bundle.getString("identityNum")
            fullName = bundle.getString("fullName")
            type = bundle.getString("type") // myiC, driving_license
        }
        tvMessage.text = message
        if (code == "0") {
            tvFullName.visibility = View.VISIBLE
            tvIdentityNum.visibility = View.VISIBLE
            tvType.visibility = View.VISIBLE
            tvFullName.text = fullName
            tvIdentityNum.text = identityNum
            tvType.text = type

            imageCaptureGenerate()
            imgImage.visibility = View.VISIBLE
        }

        tvBack.setOnClickListener {
            finishWithResult()
        }
    }

    private fun imageCaptureGenerate() {
        if (imagePath == "") {
            btBitmap = null
        } else {
            try {
                imgImage.setImageURI(Uri.fromFile(File(imagePath)))
            } catch(e: Exception) {
                btBitmap = null
            }
        }
    }

    private fun finishWithResult() {
        val intent = Intent().apply {
            putExtra("code", code)
            putExtra("message", message)
            putExtra("imagePath", imagePath)
            putExtra("identityNum", identityNum)
            putExtra("fullName", fullName)
            putExtra("type", type)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}