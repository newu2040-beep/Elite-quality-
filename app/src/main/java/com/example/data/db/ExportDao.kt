package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportDao {
    @Query("SELECT * FROM exports ORDER BY timestamp DESC")
    fun getAllExports(): Flow<List<ExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportEntity): Long

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun deleteExportById(id: Long)

    @Query("DELETE FROM exports")
    suspend fun deleteAllExports()
}
