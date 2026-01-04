package com.arkdev.a3dmodelapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.arkdev.a3dmodelapp.data.model.ModelData

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "ModelViewer.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_MODELS = "models"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_FILE_PATH = "file_path"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_UPLOADED_AT = "uploaded_at"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_MODELS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_FILE_PATH TEXT NOT NULL,
                $COLUMN_FILE_SIZE INTEGER,
                $COLUMN_UPLOADED_AT INTEGER
            )
        """.trimIndent()

        try {
            db?.execSQL(createTable)
            Log.d(TAG, "Database table created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database table", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MODELS")
        onCreate(db)
    }

    fun insertModel(name: String, description: String, filePath: String, fileSize: Long): Long {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NAME, name)
                put(COLUMN_DESCRIPTION, description)
                put(COLUMN_FILE_PATH, filePath)
                put(COLUMN_FILE_SIZE, fileSize)
                put(COLUMN_UPLOADED_AT, System.currentTimeMillis())
            }

            val id = db.insert(TABLE_MODELS, null, values)
            Log.d(TAG, "Inserted model with ID: $id, path: $filePath")

            db.close()
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting model", e)
            -1
        }
    }

    fun getAllModels(): List<ModelData> {
        val list = mutableListOf<ModelData>()

        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_MODELS ORDER BY $COLUMN_UPLOADED_AT DESC",
                null
            )

            while (cursor.moveToNext()) {
                list.add(ModelData(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    description = cursor.getString(2),
                    filePath = cursor.getString(3),
                    fileSize = cursor.getLong(4),
                    uploadedAt = cursor.getLong(5)
                ))
            }

            cursor.close()
            db.close()

            Log.d(TAG, "Retrieved ${list.size} models")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all models", e)
        }

        return list
    }

    fun getModelById(id: Int): ModelData? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_MODELS,
                null,
                "$COLUMN_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )

            var model: ModelData? = null
            if (cursor.moveToFirst()) {
                model = ModelData(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    description = cursor.getString(2),
                    filePath = cursor.getString(3),
                    fileSize = cursor.getLong(4),
                    uploadedAt = cursor.getLong(5)
                )
                Log.d(TAG, "Found model: ${model.name}, path: ${model.filePath}")
            } else {
                Log.w(TAG, "Model not found with ID: $id")
            }

            cursor.close()
            db.close()
            model
        } catch (e: Exception) {
            Log.e(TAG, "Error getting model by ID", e)
            null
        }
    }

    fun deleteModel(id: Int): Int {
        return try {
            val db = writableDatabase
            val result = db.delete(TABLE_MODELS, "$COLUMN_ID = ?", arrayOf(id.toString()))
            db.close()
            Log.d(TAG, "Deleted model with ID: $id")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model", e)
            -1
        }
    }
}
