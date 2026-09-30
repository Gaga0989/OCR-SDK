package com.css.ocr_scanner

import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.FileOutputStream

class OCRActivity : AppCompatActivity() {
    private var imagePath: String = ""
    private var imgOrgBitMap: Bitmap? = null
    private var imgBitMap: Bitmap? = null
    private var intColorLight: Int = 0
    private var strType:String = ""
    private var strIdentityNum:String = ""
    private var strFullName:String = ""
    private var rect:Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocractivity)

        val bundle = intent.extras
        if (bundle != null) {
            imagePath = bundle.getString("imagePath").toString()
        } else {
            errorFinish("1000", "(1001) OCR internal code error.")
            return
        }

        if (imagePath != "") {
            imageRectify()
        } else {
            errorFinish("1000", "(1002) OCR internal code error.")
        }
    }

    private fun imageRectify() {
        if (imagePath == null) {
            errorFinish("1000", "(1002) OCR internal code error.")
            return
        }
        imgOrgBitMap = compressImageSize(imagePath, 2.0)
        if (imgOrgBitMap == null) {
            return
        }
        checkCard()
    }

    private fun checkCard() {
        intColorLight = -8000000
        imgBitMap = imgOrgBitMap
        recognizeCard(imgBitMap!!)
    }
    private fun recognizeCard(bitmap: Bitmap) {
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
                errorFinish("1000", "(1004) " + e.message.toString())
            }
    }
    private fun editBitmap() {
        intColorLight += 1000000
        if (intColorLight >= -4000000) { // cannot read
            //intentNextActivity() // unable to read
            return
        }
        imgBitMap = imgOrgBitMap
        if (strType == "myic") {
            recognizeMyIC(imgBitMap!!)
        } else if (strType == "driving") {
            recognizeTextMalaysiaDrivingLicense(imgBitMap!!)
        } else { //driving_new1
            recognizeTextMalaysiaDrivingLicense_New1(imgBitMap!!)
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
                        Log.e("ocr_ic", "IC_FOUND:" + strIdentityNum)
                        rect = block.boundingBox
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
                successFinish()
            }
            .addOnFailureListener { e ->
                errorFinish("1000", "(1003A) " + e.message.toString())
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
                successFinish()
            }
            .addOnFailureListener { e ->
                errorFinish("1000", "(1003B) " + e.message.toString())
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
                successFinish()
            }
            .addOnFailureListener { e ->
                errorFinish("1000", "(1003C) " + e.message.toString())
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
            image = cropImage(image)

            // Save image to target filePath
            saveImageFile(filePath, image)
            return image
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            return image
        }
    }
    private fun saveImageFile(filePath:String, _bitmap: Bitmap) {
        val bitmap = addWaterMark(_bitmap)

        val fileOutputStream = FileOutputStream(filePath)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream) //JPEG
        fileOutputStream.close()
    }
    private fun addWaterMark(_bitmap:Bitmap): Bitmap {
        val waterMark = BitmapFactory.decodeResource(resources,R.drawable.watermark)
        val btBitmap = Bitmap.createBitmap(_bitmap.width, _bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(btBitmap)
        val resizeWaterMark = Bitmap.createScaledBitmap(waterMark, _bitmap.width, _bitmap.height, true)
        resizeWaterMark.setHasAlpha(true)
        canvas.drawBitmap(_bitmap, 0f, 0f, null)
        canvas.drawBitmap(resizeWaterMark, 0f, 0f, null)
        // Cover IC Number
        if (rect != null) {
            val paint = Paint().apply {
                color = Color.BLACK          // 纯黑色
                style = Paint.Style.FILL     // 填满
            }
            val halfWidth = rect!!.width() / 2          // 宽度的一半
            val blackRect = Rect(
                rect!!.left,                        // 左上角 X
                rect!!.top,                         // 左上角 Y
                rect!!.left + halfWidth,            // 右边 = 左边 + 50%
                rect!!.bottom                       // 底部 Y
            )
            canvas.drawRect(blackRect, paint)
        }

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
        val left = halfWidth - (halfWidth / CaptureActivity.left1) * CaptureActivity.left2
        val top = halfHeight - (halfHeight / CaptureActivity.top1) * CaptureActivity.top2
        val right = halfWidth + (halfWidth / CaptureActivity.right1) * CaptureActivity.right2
        val bottom = halfHeight + (halfHeight / CaptureActivity.bottom1) * CaptureActivity.bottom2
        var boxHeight = bottom - top
        var boxWidth = right - left
        return Bitmap.createBitmap(source, left, top, boxWidth, boxHeight)
    }

    private fun successFinish() {
        if (strFullName == "" && strIdentityNum == "") {
            errorFinish("1000", "(1009) unable retrieve identity number and full name")
            return
        }
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("code", "0")
            .putExtra("message", "")
            .putExtra("imagePath", imagePath)
            .putExtra("identityNum", strIdentityNum)
            .putExtra("fullName", strFullName)
            .putExtra("type", strType)
        startActivity(intent)
        return
    }
    private fun errorFinish(code:String, message: String) {
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("code", code)
            .putExtra("message", message)
        startActivity(intent)
        return
    }
}