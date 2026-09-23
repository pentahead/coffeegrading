package id.my.faruq.coffegrader.util

import android.graphics.Bitmap
import kotlin.math.roundToInt

object BitmapUtils {

    /** Skala proporsional agar sisi terpanjang = [maxSide] (px). */
    fun scaleToMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }
}
