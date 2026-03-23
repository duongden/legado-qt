package io.legado.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.WriterException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.king.zxing.DecodeFormatManager
import java.util.EnumMap
import kotlin.math.max


@Suppress("MemberVisibilityCanBePrivate", "unused")
object QRCodeUtils {

    const val DEFAULT_REQ_WIDTH = 480
    const val DEFAULT_REQ_HEIGHT = 640

    /**
     * Generate QR code
     * @param content Content of QR code
     * @param heightPix Height of QR code
     * @param logo Logo in the center of QR code
     * @param ratio Logo ratio, because max error correction rate of QR code is 30%, suggested ratio < 0.3
     * @param errorCorrectionLevel
     */
    fun createQRCode(
        content: String,
        heightPix: Int = DEFAULT_REQ_HEIGHT,
        logo: Bitmap? = null,
        @FloatRange(from = 0.0, to = 1.0) ratio: Float = 0.2f,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.H
    ): Bitmap? {
        // Config parameters
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        // Error correction level
        hints[EncodeHintType.ERROR_CORRECTION] = errorCorrectionLevel
        // Set width of blank margin
        hints[EncodeHintType.MARGIN] = 1 //default is 4
        return createQRCode(content, heightPix, logo, ratio, hints)
    }

    /**
     * Generate QR code
     * @param content Content of QR code
     * @param heightPix Height of QR code
     * @param logo Logo in the center of QR code
     * @param ratio Logo ratio, because max error correction rate of QR code is 30%, suggested ratio < 0.3
     * @param hints
     * @param codeColor Color of QR code
     * @return
     */
    fun createQRCode(
        content: String?,
        heightPix: Int,
        logo: Bitmap?,
        @FloatRange(from = 0.0, to = 1.0) ratio: Float = 0.2f,
        hints: Map<EncodeHintType, *>,
        codeColor: Int = Color.BLACK
    ): Bitmap? {
        try {
            // Image data conversion, used matrix conversion
            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, heightPix, heightPix, hints)
            val pixels = IntArray(heightPix * heightPix)
            // Here generate QR code image pixel by pixel according to algorithm
            // Two for loops are result of image row scanning
            for (y in 0 until heightPix) {
                for (x in 0 until heightPix) {
                    if (bitMatrix[x, y]) {
                        pixels[y * heightPix + x] = codeColor
                    } else {
                        pixels[y * heightPix + x] = Color.WHITE
                    }
                }
            }

            // Format of generated QR code image
            var bitmap: Bitmap? = Bitmap.createBitmap(heightPix, heightPix, Bitmap.Config.ARGB_8888)
            bitmap!!.setPixels(pixels, 0, heightPix, 0, 0, heightPix, heightPix)
            if (logo != null) {
                bitmap = addLogo(bitmap, logo, ratio)
            }
            return bitmap
        } catch (e: WriterException) {
            e.printOnDebug()
        }
        return null
    }

    /**
     * Add Logo image in the center of QR code
     * @param src
     * @param logo
     * @param ratio Logo ratio, because max error correction rate of QR code is 30%, suggested ratio < 0.3
     * @return
     */
    private fun addLogo(
        src: Bitmap?,
        logo: Bitmap?,
        @FloatRange(from = 0.0, to = 1.0) ratio: Float
    ): Bitmap? {
        if (src == null) {
            return null
        }
        if (logo == null) {
            return src
        }

        // Get image width and height
        val srcWidth = src.width
        val srcHeight = src.height
        val logoWidth = logo.width
        val logoHeight = logo.height
        if (srcWidth == 0 || srcHeight == 0) {
            return null
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src
        }

        // Logo size relative to overall QR code size
        val scaleFactor = srcWidth * ratio / logoWidth
        var bitmap: Bitmap? = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            canvas.scale(
                scaleFactor,
                scaleFactor,
                (srcWidth / 2).toFloat(),
                (srcHeight / 2).toFloat()
            )
            canvas.drawBitmap(
                logo,
                ((srcWidth - logoWidth) / 2).toFloat(),
                ((srcHeight - logoHeight) / 2).toFloat(),
                null
            )
            canvas.save()
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.printOnDebug()
        }
        return bitmap
    }

    /**
     * Parse barcode/QR code
     * @param bitmap Image
     * @param hints Hints
     * @return
     */
    fun parseCode(
        bitmap: Bitmap,
        reqWidth: Int = DEFAULT_REQ_WIDTH,
        reqHeight: Int = DEFAULT_REQ_HEIGHT,
        hints: Map<DecodeHintType?, Any?> = DecodeFormatManager.ALL_HINTS
    ): String? {
        val result = parseCodeResult(bitmap, reqWidth, reqHeight, hints)
        return result?.text
    }

    /**
     * Parse 1D/QR code image
     * @param bitmap Image to parse
     * @param hints Decode hint types
     * @return
     */
    fun parseCodeResult(
        bitmap: Bitmap,
        reqWidth: Int = DEFAULT_REQ_WIDTH,
        reqHeight: Int = DEFAULT_REQ_HEIGHT,
        hints: Map<DecodeHintType?, Any?> = DecodeFormatManager.ALL_HINTS
    ): Result? {
        if (bitmap.width > reqWidth || bitmap.height > reqHeight) {
            val bm = bitmap.resizeAndRecycle(reqWidth, reqHeight)
            return parseCodeResult(getRGBLuminanceSource(bm), hints)
        }
        return parseCodeResult(getRGBLuminanceSource(bitmap), hints)
    }

    /**
     * Parse 1D/QR code image
     * @param source
     * @param hints
     * @return
     */
    fun parseCodeResult(source: LuminanceSource?, hints: Map<DecodeHintType?, Any?>?): Result? {
        var result: Result? = null
        val reader = MultiFormatReader()
        try {
            reader.setHints(hints)
            if (source != null) {
                result = decodeInternal(reader, source)
                if (result == null) {
                    result = decodeInternal(reader, source.invert())
                }
                if (result == null && source.isRotateSupported) {
                    result = decodeInternal(reader, source.rotateCounterClockwise())
                }
            }
        } catch (e: java.lang.Exception) {
            e.printOnDebug()
        } finally {
            reader.reset()
        }
        return result
    }

    /**
     * Parse QR code
     * @param bitmapPath Image path
     * @return
     */
    fun parseQRCode(bitmapPath: String?): String? {
        val result = parseQRCodeResult(bitmapPath)
        return result?.text
    }

    /**
     * Parse QR code image
     * @param bitmapPath Image path to parse
     * @param reqWidth Request target width, compress if actual width is larger. No compression if <= 0
     * @param reqHeight Request target height, compress if actual height is larger. No compression if <= 0
     * @return
     */
    fun parseQRCodeResult(
        bitmapPath: String?,
        reqWidth: Int = DEFAULT_REQ_WIDTH,
        reqHeight: Int = DEFAULT_REQ_HEIGHT
    ): Result? {
        return parseCodeResult(bitmapPath, reqWidth, reqHeight, DecodeFormatManager.QR_CODE_HINTS)
    }

    /**
     * Parse barcode/QR code
     * @param bitmapPath Image path
     * @return
     */
    fun parseCode(
        bitmapPath: String?,
        reqWidth: Int = DEFAULT_REQ_WIDTH,
        reqHeight: Int = DEFAULT_REQ_HEIGHT,
        hints: Map<DecodeHintType?, Any?> = DecodeFormatManager.ALL_HINTS
    ): String? {
        return parseCodeResult(bitmapPath, reqWidth, reqHeight, hints)?.text
    }

    /**
     * Parse 1D/QR code image
     * @param bitmapPath Image path to parse
     * @param reqWidth Request target width, compress if actual width is larger. No compression if <= 0
     * @param reqHeight Request target height, compress if actual height is larger. No compression if <= 0
     * @param hints Decode hint types
     * @return
     */
    fun parseCodeResult(
        bitmapPath: String?,
        reqWidth: Int = DEFAULT_REQ_WIDTH,
        reqHeight: Int = DEFAULT_REQ_HEIGHT,
        hints: Map<DecodeHintType?, Any?> = DecodeFormatManager.ALL_HINTS
    ): Result? {
        var result: Result? = null
        val reader = MultiFormatReader()
        try {
            reader.setHints(hints)
            val source = getRGBLuminanceSource(compressBitmap(bitmapPath, reqWidth, reqHeight))
            result = decodeInternal(reader, source)
            if (result == null) {
                result = decodeInternal(reader, source.invert())
            }
            if (result == null && source.isRotateSupported) {
                result = decodeInternal(reader, source.rotateCounterClockwise())
            }
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            reader.reset()
        }
        return result
    }

    private fun decodeInternal(reader: MultiFormatReader, source: LuminanceSource): Result? {
        var result: Result? = null
        try {
            try {
                // Parse using HybridBinarizer
                result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            } catch (_: Exception) {
            }
            if (result == null) {
                // If parse failed, try GlobalHistogramBinarizer again
                result = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
            }
        } catch (_: Exception) {
        }
        return result
    }


    /**
     * Compress image
     * @param path
     * @return
     */
    private fun compressBitmap(path: String?, reqWidth: Int, reqHeight: Int): Bitmap {
        if (reqWidth > 0 && reqHeight > 0) { // Check compression if both are larger
            val newOpts = BitmapFactory.Options()
            // Start reading image, set options.inJustDecodeBounds back to true
            newOpts.inJustDecodeBounds = true // Get original image size
            BitmapFactory.decodeFile(path, newOpts) // BM returns null at this time
            val width = newOpts.outWidth.toFloat()
            val height = newOpts.outHeight.toFloat()
            // Scale ratio, since fixed ratio scaling, only need to calculate using one of width or height
            var wSize = 1 // wSize=1 means no scaling
            if (width > reqWidth) { // If width is larger, scale based on width fixed size
                wSize = (width / reqWidth).toInt()
            }
            var hSize = 1 // wSize=1 means no scaling
            if (height > reqHeight) { // If height is larger, scale based on height fixed size
                hSize = (height / reqHeight).toInt()
            }
            var size = max(wSize, hSize)
            if (size <= 0) size = 1
            newOpts.inSampleSize = size // Set scale ratio
            // Re-read image, note options.inJustDecodeBounds is set back to false
            newOpts.inJustDecodeBounds = false
            return BitmapFactory.decodeFile(path, newOpts)
        }
        return BitmapFactory.decodeFile(path)
    }


    /**
     * Get RGBLuminanceSource
     * @param bitmap
     * @return
     */
    private fun getRGBLuminanceSource(bitmap: Bitmap): RGBLuminanceSource {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return RGBLuminanceSource(width, height, pixels)
    }

    /**
     * Generate Barcode
     * @param content
     * @param format
     * @param desiredWidth
     * @param desiredHeight
     * @param hints
     * @param isShowText
     * @param textSize
     * @param codeColor
     * @return
     */
    fun createBarCode(
        content: String?,
        desiredWidth: Int,
        desiredHeight: Int,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        hints: Map<EncodeHintType?, *>? = null,
        isShowText: Boolean = true,
        textSize: Int = 40,
        @ColorInt codeColor: Int = Color.BLACK
    ): Bitmap? {
        if (TextUtils.isEmpty(content)) {
            return null
        }
        val writer = MultiFormatWriter()
        try {
            val result = writer.encode(
                content, format, desiredWidth,
                desiredHeight, hints
            )
            val width = result.width
            val height = result.height
            val pixels = IntArray(width * height)
            // All are 0, or black, by default
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (result[x, y]) codeColor else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return if (isShowText) {
                addCode(bitmap, content, textSize, codeColor, textSize / 2)
            } else bitmap
        } catch (e: WriterException) {
            e.printOnDebug()
        }
        return null
    }

    /**
     * Add text info below barcode
     * @param src
     * @param code
     * @param textSize
     * @param textColor
     * @return
     */
    private fun addCode(
        src: Bitmap?,
        code: String?,
        textSize: Int,
        @ColorInt textColor: Int,
        offset: Int
    ): Bitmap? {
        if (src == null) {
            return null
        }
        if (TextUtils.isEmpty(code)) {
            return src
        }

        // Get image width and height
        val srcWidth = src.width
        val srcHeight = src.height
        if (srcWidth <= 0 || srcHeight <= 0) {
            return null
        }
        var bitmap: Bitmap? = Bitmap.createBitmap(
            srcWidth,
            srcHeight + textSize + offset * 2,
            Bitmap.Config.ARGB_8888
        )
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            val paint = TextPaint()
            paint.textSize = textSize.toFloat()
            paint.color = textColor
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                code!!,
                (srcWidth / 2).toFloat(),
                (srcHeight + textSize / 2 + offset).toFloat(),
                paint
            )
            canvas.save()
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.printOnDebug()
        }
        return bitmap
    }


}