package com.example.data.repository

import com.example.data.db.ExportDao
import com.example.data.db.ExportEntity
import kotlinx.coroutines.flow.Flow

class ExportRepository(private val exportDao: ExportDao) {
    val allExports: Flow<List<ExportEntity>> = exportDao.getAllExports()

    suspend fun saveExport(export: ExportEntity): Long = exportDao.insertExport(export)

    suspend fun deleteExport(id: Long) = exportDao.deleteExportById(id)

    suspend fun clearAll() = exportDao.deleteAllExports()
}
