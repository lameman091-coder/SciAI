package com.funtime.sciai.ui.theme.library

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "temp_file.pdf"
        val tempFile = File(context.cacheDir, fileName)
        
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(4 * 1024) // 4KB buffer
            var read: Int
            
            while (inputStream?.read(buffer).also { read = it ?: -1 } != -1) {
                outputStream.write(buffer, 0, read)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream?.close()
            
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex("_display_name")
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
