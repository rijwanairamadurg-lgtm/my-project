package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val result: String,
    val isBodmas: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val pageIndex: Int = 0
)

@Entity(tableName = "calculator_pages")
data class CalculatorPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pageIndex: Int,
    val title: String,
    val expression: String = "",
    val result: String = "",
    val isBodmas: Boolean = true,
    val memoryData: String = "",
    val memoryTotal: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notebook_pages")
data class NotebookPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pageIndex: Int,
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
