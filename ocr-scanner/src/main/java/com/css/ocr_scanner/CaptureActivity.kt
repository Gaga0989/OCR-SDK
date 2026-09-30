package com.css.ocr_scanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class CaptureActivity : AppCompatActivity() {
    private val btnEditFrame by lazy { findViewById<Button>(R.id.btnEditFrame) }
    private val btnCaptureConfirm by lazy { findViewById<Button>(R.id.btnCaptureConfirm) }
    private val previewCamera by lazy { findViewById<PreviewView>(R.id.previewCamera) }
    private val surfaceView by lazy { findViewById<SurfaceView>(R.id.surfaceView) }
    private val overlayLoading by lazy { findViewById<ConstraintLayout>(R.id.overlayLoading) }
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var imageCapture: ImageCapture? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var canvas: Canvas? = null
    private var paint: Paint? = null
    private var photoFile: File? = null
    companion object {
        var left1 = 0
        var left2 = 0
        var top1 = 0
        var top2 = 0
        var right1 = 0
        var right2 = 0
        var bottom1 = 0
        var bottom2 = 0
    }
    // OCR reading
    private var strImageFilePath:String = ""
    private var btCropOnlyImg: Bitmap? = null
    private var btResizeImage: Bitmap? = null
    private var intColorLight: Int = 0
    private var strType:String = ""
    private var strIdentityNum:String = ""
    private var strFullName:String = ""
    private var rect:Rect? = null
    // Final
    private var strCode:String = "0"
    private var strMessage:String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        val bundle = intent.extras
        if (bundle != null) {
            strCode = bundle.getString("code").toString()
            strMessage = bundle.getString("message").toString()
        }
        if (strCode != "0") {
            showDataDialog(strCode, strMessage)
            return
        }
        startCameraPreview()

        btnEditFrame.setOnClickListener {
            changeFrame()
        }
        btnCaptureConfirm.setOnClickListener {
            captureImageConfirm()
        }
    }

    //region Capture Image
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture!!.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewCamera.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
        surfaceView.setZOrderOnTop(true)
        surfaceHolder = surfaceView.holder
        surfaceHolder!!.setFormat(PixelFormat.TRANSPARENT)
        surfaceHolder!!.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                drawFocusRect()
            }
            override fun surfaceCreated(holder: SurfaceHolder) {
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })
    }

    private fun drawFocusRect() {
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val height: Int = previewCamera.height
        val width: Int = previewCamera.width
        var diameter = width
        if (height < width) {
            diameter = height
        }
        val offset = (0.05 * diameter).toInt()
        diameter -= offset
        canvas = surfaceHolder?.lockCanvas()
        canvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
        paint = Paint()
        paint!!.style = Paint.Style.STROKE
        paint!!.color = Color.parseColor("#6A6A6A")
        paint!!.strokeWidth = 5f
        //val left = width / 2 - diameter / 3 - 20
        // diameter 1118, width 1176
        //val imageFrame = ImageFrame()
        val halfWidth = width / 2
        val halfHeight = height / 2
        val left = halfWidth - (halfWidth / left1) * left2
        val top = halfHeight - (halfHeight / top1) * top2
        val right = halfWidth + (halfWidth / right1) * right2
        val bottom = halfHeight + (halfHeight / bottom1) * bottom2
        canvas!!.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint!!)
        surfaceHolder?.unlockCanvasAndPost(canvas)
    }
    private fun changeFrame() {
        val inflater = LayoutInflater.from(this)
        val fragmentView = inflater.inflate(R.layout.frame_setup_ui, null)
        val alertDialog = AlertDialog.Builder(this).setView(fragmentView).create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val radioGroupFrame = fragmentView.findViewById<RadioGroup>(R.id.radioGroupFrame)
        val llManualForm = fragmentView.findViewById<LinearLayout>(R.id.llManualForm)
        val txtTop1 = fragmentView.findViewById<EditText>(R.id.txtTop1)
        val txtTop2 = fragmentView.findViewById<EditText>(R.id.txtTop2)
        val txtBottom1 = fragmentView.findViewById<EditText>(R.id.txtBottom1)
        val txtBottom2 = fragmentView.findViewById<EditText>(R.id.txtBottom2)
        val txtLeft1 = fragmentView.findViewById<EditText>(R.id.txtLeft1)
        val txtLeft2 = fragmentView.findViewById<EditText>(R.id.txtLeft2)
        val txtRight1 = fragmentView.findViewById<EditText>(R.id.txtRight1)
        val txtRight2 = fragmentView.findViewById<EditText>(R.id.txtRight2)
        val tvOkDg = fragmentView.findViewById<TextView>(R.id.tvOkDg)

        radioGroupFrame.check(R.id.radioManual)
        llManualForm.visibility = View.VISIBLE
        txtTop1.setText(top1.toString())
        txtTop2.setText(top2.toString())
        txtBottom1.setText(bottom1.toString())
        txtBottom2.setText(bottom2.toString())
        txtLeft1.setText(left1.toString())
        txtLeft2.setText(left2.toString())
        txtRight1.setText(right1.toString())
        txtRight2.setText(right2.toString())
        radioGroupFrame.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radioManual) {
                llManualForm.visibility = View.VISIBLE
            } else {
                llManualForm.visibility = View.GONE
            }
        }
        tvOkDg.setOnClickListener(View.OnClickListener {
            when (radioGroupFrame.checkedRadioButtonId) {
                R.id.radioCenter -> {
                    left1 = 6
                    left2 = 5
                    top1 = 3 // 切3格子
                    top2 = 1 // 以画面中心开始计算拿1个格子
                    right1 = 6
                    right2 = 5
                    bottom1 = 3
                    bottom2 = 1
                }
                R.id.radioOpt1 -> {
                    left1 = 10
                    left2 = 9
                    top1 = 6
                    top2 = 4
                    right1 = 10
                    right2 = 9
                    bottom1 = 5
                    bottom2 = 1
                }
                R.id.radioOpt2 -> {
                    left1 = 10
                    left2 = 7
                    top1 = 6 // cut 6 part
                    top2 = 0 // take 2 part
                    right1 = 10
                    right2 = 7
                    bottom1 = 8
                    bottom2 = 5 // control bottom line, num bigger to bottom (height size)
                }
                R.id.radioOpt3 -> {
                    left1 = 6
                    left2 = 5
                    top1 = 3
                    top2 = 1
                    right1 = 6
                    right2 = 5
                    bottom1 = 3
                    bottom2 = 1
                }
                else -> {
                    left1 = txtLeft1.text.toString().trim().toInt()
                    left2 = txtLeft2.text.toString().trim().toInt()
                    top1 = txtTop1.text.toString().trim().toInt()
                    top2 = txtTop2.text.toString().trim().toInt()
                    right1 = txtRight1.text.toString().trim().toInt()
                    right2 = txtRight2.text.toString().trim().toInt()
                    bottom1 = txtBottom1.text.toString().trim().toInt()
                    bottom2 = txtBottom2.text.toString().trim().toInt()
                }
            }
            alertDialog.dismiss()
            recreate()
        })
        alertDialog.show()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
    }

    private fun captureImageConfirm() {
        overlayLoading.visibility = View.VISIBLE

        val imageCapture = imageCapture ?: return
        photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    strImageFilePath = photoFile!!.absolutePath
                    Handler(Looper.getMainLooper()).postDelayed({
                        readImageOCR()
                    }, 150)   // 150ms 延迟基本能解决文件未写完的问题
                    //readImageOCR()
                }
            })
    }

    private fun createImageFile(fileName: String = "CSS_Capture_Image"): File {
        val tempFile = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //return File.createTempFile(fileName, ".jpg", tempFile)
        return File.createTempFile(fileName, ".webp", tempFile)
    }

    private fun readImageOCR() {
        btnEditFrame.visibility = View.GONE
        btnCaptureConfirm.visibility = View.GONE

        if (strImageFilePath == null) {
            showDataDialog("1000", "(1002) OCR internal code error.")
            return
        }
        imageCompress(strImageFilePath, true)
        if (btResizeImage == null) {
            return
        }
        checkCard()
    }
    //endregion

    private fun checkCard() {
        intColorLight = -8000000
        recognizeCard(btCropOnlyImg!!)
    }
    private fun recognizeCard(bitmap: Bitmap) {
        if (bitmap == null) {
            showDataDialog("1000", "(1011) Image generate failed. Please try again")
            return
        }
        strType = "myic" // default
        var intWatcher = 0
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap!!, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    var strBlock = block.text.uppercase()
                    if (strBlock.contains("LESEN")) { //LESEN MEMANDU
                        if (strBlock.contains("SLIP") || strBlock.contains("PEMBAHARUAN")) { //SLIP / PEMBAHARUAN
                            strType = "driving_new1"
                            break
                        }
                        strType = "driving"
                        break
                    }
                    intWatcher += 1
                    if (intWatcher > 3) {
                        break
                    }
                }
                intColorLight = -8000000
                editBitmap()
            }
            .addOnFailureListener { e ->
                showDataDialog("1000", "(1004) " + e.message.toString())
            }
    }
    private fun editBitmap() {
        intColorLight += 1000000
        if (intColorLight >= -4000000) { // cannot read
            //intentNextActivity() // unable to read
            return
        }
        if (strType == "myic") {
            recognizeMyIC(btCropOnlyImg!!)
        } else if (strType == "driving") {
            recognizeTextMalaysiaDrivingLicense(btCropOnlyImg!!)
        } else { //driving_new1
            recognizeTextMalaysiaDrivingLicense_New1(btCropOnlyImg!!)
        }
    }
    private fun recognizeMyIC(bitmap: Bitmap) {
        strIdentityNum = ""
        strFullName = ""
        var blIsBreak = false
        var intBlockLineSize = 0
        val replaceRegex = Regex("[^A-Za-z0-9 -]")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap!!, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    if (blIsBreak) {
                        break
                    }
                    val strLanguageType = block.recognizedLanguage
                    var intNameLine = 0
                    Log.e("ocr_block", block.text)
                    if (strIdentityNum == "" && strLanguageType == "und" && block.text.contains("-")) {
                        strIdentityNum = block.text.replace(replaceRegex, "")
                        rect = block.boundingBox!!
                        Log.e("ocr_ic", "IC_FOUND:" + strIdentityNum)
                        continue
                    }
                    if (strIdentityNum.length == 14 && strFullName == "") {
                        var strName = ""
                        for (line in block.lines) {
                            Log.e("ocr_line", line.text)
                            strName = " " + line.text.replace(replaceRegex, "")
                            if (!strName.contains(" ")) {
                                continue
                            }
                            intBlockLineSize += 1
                            intNameLine += 1
                            if (intNameLine < 3) { // max loop 3
                                if (strName.contains("[a-z]".toRegex())) {
                                    blIsBreak = true
                                    break
                                }
                                if (line.text.contains("[0-9]".toRegex())) {
                                    blIsBreak = true
                                    break
                                }
                                if (intBlockLineSize >= block.lines.size || intNameLine == 2) {
                                    strFullName = "$strFullName $strName"
                                }
                            }
                        }
                    }
                    intBlockLineSize = 0
                }
                showDataDialog("0", "")
            }
            .addOnFailureListener { e ->
                showDataDialog("1000", "(1003A) " + e.message.toString())
            }
    }
    private fun recognizeTextMalaysiaDrivingLicense(bitmap: Bitmap) {
        strIdentityNum = ""
        strFullName = ""
        var strPreValue = ""
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap!!, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        var strValue = line.text.toString()
                        if (strPreValue.contains("LESEN")) {
                            strFullName = strValue
                        }
                        if (strPreValue.contains("DRIVING")) {
                            strFullName = strValue
                        }
                        if (strValue.contains("[0-9]".toRegex())) {
                            strValue = strValue.replace(" ","")
                            if (strValue.length == 12) { // consider ic number
                                strIdentityNum = strValue
                            }
                        }
                        strPreValue = strValue.toUpperCase()
                    }
                }
                showDataDialog("0", "")
            }
            .addOnFailureListener { e ->
                showDataDialog("1000", "(1003B) " + e.message.toString())
            }
    }
    private fun recognizeTextMalaysiaDrivingLicense_New1(bitmap: Bitmap) {
        strIdentityNum = ""
        strFullName = ""
        var strPreValue = ""
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap!!, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        var strValue = line.text.toString()
                        if (strPreValue.contains("NAMA")) {
                            strFullName = strValue
                        }
                        if (strPreValue.contains("NAME")) {
                            strFullName = strValue
                        }
                        if (strValue.contains("[0-9]".toRegex())) {
                            strValue = strValue.replace(" ","")
                            if (strValue.length >= 10) { // consider ic number
                                strIdentityNum = strValue
                                break
                            }
                        }
                        strPreValue = strValue.toUpperCase()
                    }
                }
                showDataDialog("0", "")
            }
            .addOnFailureListener { e ->
                showDataDialog("1000", "(1003C) " + e.message.toString())
            }
    }

    // Image Size (Compress, cut, and rotate)
    private fun compressImageSize(filePath: String, targetMB: Double = 1.0) : Bitmap? {
        var image: Bitmap? = null
        try {
            image = BitmapFactory.decodeFile(filePath)
            val exif = ExifInterface(filePath)
            val exifOrientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val exifDegree: Int = exifOrientationToDegrees(exifOrientation)
            image = rotateImage(image, exifDegree.toFloat())
            // Save image to target filePath
            //btCropOnlyImg = image

            image = cropImage(image) // 原始
            btCropOnlyImg = image
            return image
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            return image
        }
    }
    private fun imageCompress(strImageFilePath:String, blRotateRequest:Boolean) {
        val file = File(strImageFilePath)
        //var bitmap = BitmapFactory.decodeFile(file.absolutePath)
        var bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: run {
            showDataDialog("1000", "(1010) Unable decode image，please try again")
            return
        }
        if (blRotateRequest) {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degree = exifOrientationToDegrees(orientation)
            bitmap = rotateImage(bitmap, degree.toFloat())
        }
        bitmap = cropImage(bitmap) // 原始
        btCropOnlyImg = bitmap

        var quality = 90
        var byteArray: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            byteArray = stream.toByteArray()
            quality -= 5
        } while (byteArray.size > 150 * 1024 && quality > 10)
        if (byteArray.size > 150 * 1024) {
            bitmap = resizeBitmap(bitmap, 1000, 1000)
            quality = 90
            do {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                byteArray = stream.toByteArray()
                quality -= 5
            } while (byteArray.size > 150 * 1024 && quality > 10)
        }
        FileOutputStream(file).use {
            it.write(byteArray)
        }
        btResizeImage = bitmap
    }
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }

    private fun saveImageFile(filePath:String, _bitmap: Bitmap) {
        var file = File(filePath)
        var sizeInBytes = file.length()
        var kb = sizeInBytes / 1024.0


        var bitmap = addWaterMark(_bitmap)
        //bitmap = cropImage(bitmap)

        val fileOutputStream = FileOutputStream(filePath)
        //bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream) //JPEG
        //bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 80, fileOutputStream) // WEBP
        bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()

        // check size
        file = File(filePath)
        sizeInBytes = file.length()
        kb = sizeInBytes / 1024.0
        val sizeInKb = kb
    }
    private fun addWaterMark(_bitmap: Bitmap): Bitmap {
        val waterMark = BitmapFactory.decodeResource(resources, R.drawable.watermark)

        val btBitmap = Bitmap.createBitmap(
            _bitmap.width,
            _bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(btBitmap)

        // 1. 画原图
        canvas.drawBitmap(_bitmap, 0f, 0f, null)

        // 2. Cover IC Number（保留你原来的遮盖逻辑）
        if (rect != null) {
            val paint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            val coverRect = Rect(
                rect!!.left,
                rect!!.top,
                rect!!.left + (rect!!.width() * 0.5f).toInt(),
                rect!!.bottom
            )
            canvas.drawRect(coverRect, paint)
        }

        // 3. 水印按比例缩小（推荐大小，可自行调整）
        val targetWidth = (_bitmap.width * 0.35).toInt()   // 占图片宽度的 25%（可改 0.2f ~ 0.35f）
        val targetHeight = (targetWidth * waterMark.height / waterMark.width.toFloat()).toInt()

        val resizedWaterMark = Bitmap.createScaledBitmap(waterMark, targetWidth, targetHeight, true)
        resizedWaterMark.setHasAlpha(true)

        // ===== 关键修改：放到正中间 =====
        val left = (_bitmap.width - resizedWaterMark.width) / 2
        val top = (_bitmap.height - resizedWaterMark.height) / 2

        canvas.drawBitmap(resizedWaterMark, left.toFloat(), top.toFloat(), null)

        // 回收内存
        resizedWaterMark.recycle()
        waterMark.recycle() // 如果后面不再用到

        return btBitmap
    }

    private fun addWaterMark_old(_bitmap:Bitmap): Bitmap {
        val waterMark = BitmapFactory.decodeResource(resources,R.drawable.watermark)
        val wmWidth = (_bitmap.width * 0.25).toInt()
        val wmHeight = waterMark.height * wmWidth / waterMark.width
        val btBitmap = Bitmap.createBitmap(_bitmap.width, _bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(btBitmap)
        canvas.drawBitmap(_bitmap, 0f, 0f, null)
        // Cover IC Number
        if (rect != null) {
            val paint = Paint().apply {
                color = Color.BLACK          // 纯黑色
                style = Paint.Style.FILL     // 填满
            }
            var ooo = rect!!.left //360
            var aaa = rect!!.top // 317
            var ddd = rect!!.bottom // 378
            var eee = rect!!.width() // 448
            var yyy = rect!!.height() // 61
            var btwidth = _bitmap.width //
            var btheight = _bitmap.height //
            val coverRect = Rect(
                rect!!.left,
                rect!!.top,
                rect!!.left + (rect!!.width() * 0.5f).toInt(),
                rect!!.bottom
            )
            canvas.drawRect(coverRect, paint)
        }
        // Watermark edit
        val resizeWaterMark = Bitmap.createScaledBitmap(waterMark, _bitmap.width, _bitmap.height, true)
        resizeWaterMark.setHasAlpha(true)
        canvas.drawBitmap(resizeWaterMark, 0f, 0f, null)

        return btBitmap
    }

    private fun exifOrientationToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                90
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                180
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                270
            }
            else -> 0
        }
    }
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    private fun cropImage(source: Bitmap):Bitmap {
        val height: Int = source.height
        val width: Int = source.width
        var diameter = width
        if (height < width) {
            diameter = height
        }
        val offset = (0.05 * diameter).toInt()
        diameter -= offset
        val halfWidth = width / 2
        val halfHeight = height / 2
        val left = halfWidth - (halfWidth / left1) * left2
        val top = halfHeight - (halfHeight / top1) * top2
        val right = halfWidth + (halfWidth / right1) * right2
        val bottom = halfHeight + (halfHeight / bottom1) * bottom2
        var boxHeight = bottom - top
        var boxWidth = right - left
        return Bitmap.createBitmap(source, left, top, boxWidth, boxHeight)
    }

    private fun showDataDialog(_code:String, _message:String, displayDialog:Boolean = false) {
        try {
            saveImageFile(strImageFilePath, btResizeImage!!) // 储存一个 water mark 和 cover
        } catch (e: Exception) {

        }
        if (!displayDialog) {
            finishWithResult()
            return
        }

        overlayLoading.visibility = View.GONE
        strCode = _code
        strMessage = _message

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.data_dialog, null)
        val alertDialog = AlertDialog.Builder(this).setView(view).create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvFullName = view.findViewById<TextView>(R.id.tvFullName)
        val tvIdentityNum = view.findViewById<TextView>(R.id.tvIdentityNum)
        val tvType = view.findViewById<TextView>(R.id.tvType)
        val imgImage = view.findViewById<ImageView>(R.id.imgImage)
        val tvBack = view.findViewById<TextView>(R.id.tvBack)

        if (_code != "0") {
            tvMessage.text = strMessage
        } else {
            if (strFullName == "" && strIdentityNum == "") {
                strCode = "1000"
                strMessage = "(1009) unable retrieve identity number and full name"
                tvMessage.text = strMessage
            } else {
                tvFullName.visibility = View.VISIBLE
                tvIdentityNum.visibility = View.VISIBLE
                tvType.visibility = View.VISIBLE
                imgImage.visibility = View.VISIBLE
                tvFullName.text = strFullName
                tvIdentityNum.text = strIdentityNum
                tvType.text = strType

                try {
                    imgImage.setImageURI(Uri.fromFile(File(strImageFilePath)))
                } catch(e: Exception) {

                }
                imgImage.visibility = View.VISIBLE
            }
        }
        tvBack.setOnClickListener(View.OnClickListener {
            alertDialog.dismiss()
            finishWithResult()
        })
        alertDialog.show()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
    }

    private fun finishWithResult() {
        val intent = Intent().apply {
            putExtra("code", strCode)
            putExtra("message", strMessage)
            putExtra("imagePath", strImageFilePath)
            putExtra("identityNum", strIdentityNum)
            putExtra("fullName", strFullName)
            putExtra("type", type)

            putExtra("left1", left1)
            putExtra("left2", left2)
            putExtra("top1", top1)
            putExtra("top2", top2)
            putExtra("right1", right1)
            putExtra("right2", right2)
            putExtra("bottom1", bottom1)
            putExtra("bottom2", bottom2)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}