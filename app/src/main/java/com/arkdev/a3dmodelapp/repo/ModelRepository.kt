package com.arkdev.a3dmodelapp.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arkdev.a3dmodelapp.data.model.ModelData
import com.arkdev.a3dmodelapp.database.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ModelRepository(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)

    companion object {
        private const val TAG = "ModelRepository"
        private const val BUFFER_SIZE = 8192
    }

    fun addModel(name: String, description: String, uri: Uri): Long {
        var destFile: File? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        return try {
            Log.d(TAG, "=== Starting Model Upload ===")
            Log.d(TAG, "URI: $uri")

            // Step 1: Open input stream
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream from URI")
                return -1
            }

            Log.d(TAG, "Input stream opened successfully")

            // Step 2: Create destination file
            val fileName = "${System.currentTimeMillis()}.glb"
            destFile = File(context.filesDir, fileName)

            // Ensure parent directory exists
            destFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            Log.d(TAG, "Destination: ${destFile.absolutePath}")
            Log.d(TAG, "Files dir: ${context.filesDir.absolutePath}")

            // Step 3: Copy file with proper stream handling
            outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesCopied = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesCopied += bytesRead
            }

            // Force write to disk
            outputStream.flush()
            outputStream.fd.sync()

            Log.d(TAG, "Bytes copied: $bytesCopied")

            // Close streams immediately after copying
            inputStream.close()
            outputStream.close()

            // Step 4: Verify file
            if (!destFile.exists()) {
                Log.e(TAG, "File does not exist after copy!")
                return -1
            }

            val fileSize = destFile.length()
            if (fileSize == 0L) {
                Log.e(TAG, "File is empty!")
                destFile.delete()
                return -1
            }

            if (fileSize != bytesCopied) {
                Log.e(TAG, "File size mismatch! Expected: $bytesCopied, Got: $fileSize")
                destFile.delete()
                return -1
            }

            Log.d(TAG, "File saved successfully")
            Log.d(TAG, "File size: $fileSize bytes")
            Log.d(TAG, "File readable: ${destFile.canRead()}")

            // Step 5: Insert into database
            val modelId = dbHelper.insertModel(
                name = name,
                description = description,
                filePath = destFile.absolutePath,
                fileSize = fileSize
            )

            if (modelId <= 0) {
                Log.e(TAG, "Database insert failed")
                destFile.delete()
                return -1
            }

            Log.d(TAG, "=== Model Upload Successful ===")
            Log.d(TAG, "Model ID: $modelId")

            modelId

        } catch (e: Exception) {
            Log.e(TAG, "Exception in addModel", e)
            e.printStackTrace()

            destFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.d(TAG, "Cleaned up file after error")
                }
            }

            -1
        } finally {
            // Ensure streams are closed
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing streams", e)
            }
        }
    }

    fun getAllModels(): List<ModelData> {
        return try {
            val models = dbHelper.getAllModels()
            Log.d(TAG, "Retrieved ${models.size} models")

            // Verify files exist
            models.filter { model ->
                val file = File(model.filePath)
                val exists = file.exists()
                if (!exists) {
                    Log.w(TAG, "Model ${model.id} file missing: ${model.filePath}")
                }
                exists
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting models", e)
            emptyList()
        }
    }

    fun getModelById(id: Int): ModelData? {
        return try {
            val model = dbHelper.getModelById(id)

            if (model != null) {
                val file = File(model.filePath)
                if (!file.exists()) {
                    Log.e(TAG, "Model file not found: ${model.filePath}")
                    return null
                }
                Log.d(TAG, "Model found: ${model.name}, Size: ${file.length()} bytes")
            }

            model
        } catch (e: Exception) {
            Log.e(TAG, "Error getting model by id", e)
            null
        }
    }

    fun deleteModel(model: ModelData) {
        try {
            val file = File(model.filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "File deleted: $deleted")
            }

            dbHelper.deleteModel(model.id)
            Log.d(TAG, "Model deleted: ${model.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model", e)
        }
    }
}
