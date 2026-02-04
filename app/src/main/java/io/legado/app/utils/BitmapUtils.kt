@file:Suppress("unused")

package io.legado.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.android.renderscript.Toolkit
import java.io.*
import kotlin.math.*


@Suppress("WeakerAccess", "MemberVisibilityCanBePrivate")
object BitmapUtils {

    /**
     * Get image info from path, when converting image to Bitmap via BitmapFactory.decodeFile(String path),
     * We often encounter OOM (Out Of Memory) issues with large images. So we use BitmapFactory.Options mentioned above.
     *
     * @param path   File path
     * @param width  Desired image width
     * @param height Desired image height
     * @return
     */
    @Throws(IOException::class)
    fun decodeBitmap(path: String, width: Int, height: Int? = null): Bitmap? {
        val fis = FileInputStream(path)
        return fis.use {
            val op = BitmapFactory.Options()
            // If inJustDecodeBounds is set to true, only return actual width and height of image, assigned to opts.outWidth, opts.outHeight;
            op.inJustDecodeBounds = true
            BitmapFactory.decodeFileDescriptor(fis.fd, null, op)
            op.inSampleSize = calculateInSampleSize(op, width, height)
            op.inJustDecodeBounds = false
            BitmapFactory.decodeFileDescriptor(fis.fd, null, op)
        }
    }

    /**
     * Calculate InSampleSize. Default return 1
     * @param options BitmapFactory.Options,
     * @param width  Desired image width
     * @param height Desired image height
     * @return
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        width: Int? = null,
        height: Int? = null
    ): Int {
        // Get ratio size
        val wRatio = width?.let { options.outWidth / it } ?: -1
        val hRatio = height?.let { options.outHeight / it } ?: -1
        // If exceeds specified size, shrink by corresponding ratio
        return when {
            wRatio > 1 && hRatio > 1 -> max(wRatio, hRatio)
            wRatio > 1 -> wRatio
            hRatio > 1 -> hRatio
            else -> 1
        }
    }

    /** Get Bitmap image from path
     * @param path Image path
     * @return
     */
    @Throws(IOException::class)
    fun decodeBitmap(path: String): Bitmap? {
        val fis = FileInputStream(path)
        return fis.use {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true

            BitmapFactory.decodeFileDescriptor(fis.fd, null, opts)
            opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128)
            opts.inJustDecodeBounds = false
            BitmapFactory.decodeFileDescriptor(fis.fd, null, opts)
        }
    }

    /**
     * Read local resource image in most memory-efficient way
     * @param context Device context
     * @param resId Resource ID
     * @return
     */
    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opt = BitmapFactory.Options()
        opt.inPreferredConfig = Config.RGB_565
        return BitmapFactory.decodeResource(context.resources, resId, opt)
    }

    /**
     * @param context Device context
     * @param resId Resource ID
     * @param width
     * @param height
     * @return
     */
    fun decodeBitmap(context: Context, resId: Int, width: Int, height: Int): Bitmap? {
        val op = BitmapFactory.Options()
        // If inJustDecodeBounds is set to true, only return actual width and height of image, assigned to opts.outWidth, opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, resId, op) // Get size info
        op.inSampleSize = calculateInSampleSize(op, width, height)
        op.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(context.resources, resId, op)
    }

    /**
     * @param context Device context
     * @param fileNameInAssets File name in Assets
     * @param width Image width
     * @param height Image height
     * @return Bitmap
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeAssetsBitmap(
        context: Context,
        fileNameInAssets: String,
        width: Int,
        height: Int
    ): Bitmap? {
        var inputStream = context.assets.open(fileNameInAssets)
        return inputStream.use {
            val op = BitmapFactory.Options()
            // If inJustDecodeBounds is set to true, only return actual width and height of image, assigned to opts.outWidth, opts.outHeight;
            op.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, op) // Get size info
            op.inSampleSize = calculateInSampleSize(op, width, height)
            inputStream = context.assets.open(fileNameInAssets)
            op.inJustDecodeBounds = false
            BitmapFactory.decodeStream(inputStream, null, op)
        }
    }

    /**
     * @param options
     * @param minSideLength
     * @param maxNumOfPixels
     * @return
     * Setting appropriate inSampleSize is one key to solve this. BitmapFactory.Options provides another member inJustDecodeBounds.
     * After setting inJustDecodeBounds to true, decodeFile does not allocate space, but can calculate original image length and width, i.e. opts.width and opts.height.
     * With these two parameters, through a certain algorithm, an appropriate inSampleSize can be obtained.
     * Checking Android source code, Android provides this dynamic calculation method below.
     */
    fun computeSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {
        val initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels)
        var roundedSize: Int
        if (initialSize <= 8) {
            roundedSize = 1
            while (roundedSize < initialSize) {
                roundedSize = roundedSize shl 1
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8
        }
        return roundedSize
    }


    private fun computeInitialSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {

        val w = options.outWidth.toDouble()
        val h = options.outHeight.toDouble()

        val lowerBound = when (maxNumOfPixels) {
            -1 -> 1
            else -> ceil(sqrt(w * h / maxNumOfPixels)).toInt()
        }

        val upperBound = when (minSideLength) {
            -1 -> 128
            else -> min(
                floor(w / minSideLength),
                floor(h / minSideLength)
            ).toInt()
        }

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound
        }

        return when {
            maxNumOfPixels == -1 && minSideLength == -1 -> {
                1
            }
            minSideLength == -1 -> {
                lowerBound
            }
            else -> {
                upperBound
            }
        }
    }

    /**
     * Convert Bitmap to InputStream
     *
     * @param bitmap
     * @return
     */
    fun toInputStream(bitmap: Bitmap): InputStream {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90 /*ignored for PNG*/, bos)
        return ByteArrayInputStream(bos.toByteArray()).also { bos.close() }
    }

}

/**
 * Get image with specified width and height
 */
fun Bitmap.resizeAndRecycle(newWidth: Int, newHeight: Int): Bitmap {
    // Get new bitmap
    val bitmap = Toolkit.resize(this, newWidth, newHeight)
    recycle()
    return bitmap
}

/**
 * Gaussian blur
 */
fun Bitmap.stackBlur(radius: Int = 8): Bitmap {
    return Toolkit.blur(this, radius)
}

/**
 * Get average color
 */
fun Bitmap.getMeanColor(): Int {
    val width: Int = this.width
    val height: Int = this.height
    var pixel: Int
    var pixelSumRed = 0
    var pixelSumBlue = 0
    var pixelSumGreen = 0
    for (i in 0..99) {
        for (j in 70..99) {
            pixel = this.getPixel(
                (i * width / 100.toFloat()).roundToInt(),
                (j * height / 100.toFloat()).roundToInt()
            )
            pixelSumRed += Color.red(pixel)
            pixelSumGreen += Color.green(pixel)
            pixelSumBlue += Color.blue(pixel)
        }
    }
    val averagePixelRed = pixelSumRed / 3000
    val averagePixelBlue = pixelSumBlue / 3000
    val averagePixelGreen = pixelSumGreen / 3000
    return Color.rgb(
        averagePixelRed + 3,
        averagePixelGreen + 3,
        averagePixelBlue + 3
    )

}
