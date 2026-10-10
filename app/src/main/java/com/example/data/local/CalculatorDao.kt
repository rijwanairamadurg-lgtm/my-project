package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculatorDao {
    // History
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistoryEntity>>

    @Query("SELECT * FROM calculation_history WHERE expression LIKE '%' || :query || '%' OR result LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<CalculationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CalculationHistoryEntity): Long

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAllHistory()

    // Pages
    @Query("SELECT * FROM calculator_pages ORDER BY pageIndex ASC")
    fun getAllPages(): Flow<List<CalculatorPageEntity>>

    @Query("SELECT * FROM calculator_pages ORDER BY pageIndex ASC")
    suspend fun getAllPagesList(): List<CalculatorPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: CalculatorPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<CalculatorPageEntity>)

    @Update
    suspend fun updatePage(page: CalculatorPageEntity)

    @Query("DELETE FROM calculator_pages WHERE id = :id")
    suspend fun deletePageById(id: Long)

    @Query("DELETE FROM calculator_pages")
    suspend fun clearAllPages()

    // Notebook Pages
    @Query("SELECT * FROM notebook_pages ORDER BY pageIndex ASC")
    fun getAllNotebookPages(): Flow<List<NotebookPageEntity>>

    @Query("SELECT * FROM notebook_pages ORDER BY pageIndex ASC")
    suspend fun getAllNotebookPagesList(): List<NotebookPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebookPage(page: NotebookPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebookPages(pages: List<NotebookPageEntity>)

    @Update
    suspend fun updateNotebookPage(page: NotebookPageEntity)

    @Query("DELETE FROM notebook_pages WHERE id = :id")
    suspend fun deleteNotebookPageById(id: Long)

    @Query("DELETE FROM notebook_pages")
    suspend fun clearAllNotebookPages()
}
