package com.ndev.moodyroutine.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageHelper {

    private const val RELATIVE_SUBFOLDER = "MoodyRoutine"

    suspend fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/$RELATIVE_SUBFOLDER"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Could not create MediaStore entry in Downloads"))

                resolver.openOutputStream(uri, "wt")?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                    os.flush()
                } ?: return@withContext Result.failure(Exception("Could not open output stream for $fileName"))

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                val displayPath = "Downloads/$RELATIVE_SUBFOLDER/$fileName"
                AppLogger.i("StorageHelper", "Successfully saved $fileName to $displayPath")
                Result.success("Saved to $displayPath")
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    RELATIVE_SUBFOLDER
                )
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val targetFile = File(dir, fileName)
                targetFile.writeText(content, Charsets.UTF_8)

                val displayPath = "Downloads/$RELATIVE_SUBFOLDER/$fileName"
                AppLogger.i("StorageHelper", "Successfully saved $fileName to $displayPath")
                Result.success("Saved to $displayPath")
            }
        } catch (e: Exception) {
            AppLogger.e("StorageHelper", "Failed to save $fileName to storage", e)
            Result.failure(e)
        }
    }
}
