package com.arkdev.a3dmodelapp.data.model

data class ModelData(
    val id: Int = 0,
    val name: String,
    val description: String,
    val filePath: String,
    val fileSize: Long,
    val uploadedAt: Long
)
