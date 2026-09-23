package id.my.faruq.coffegrader.camera

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CameraCaptureManager {

    suspend fun saveBitmap(context: Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        file
    }
}
