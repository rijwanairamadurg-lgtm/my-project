package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CalculationHistoryEntity
import com.example.data.local.CalculatorPageEntity
import com.example.data.local.NotebookPageEntity
import com.example.data.repository.AppLanguage
import com.example.data.repository.AppTheme
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import com.example.engine.CalculatorEngine
import com.example.util.AgeCalculatorEngine
import com.example.util.IndianNumberToWords
import com.example.util.Localization
import com.example.util.UnitConverterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalculatorPage(
    val id: Long = 0,
    val title: String,
    val expression: String = "",
    val previewResult: String = "",
    val finalResult: String = "",
    val isBodmas: Boolean = true,
    val memoryItems: List<MemoryItem> = emptyList(),
    val memoryTotalFormatted: String = "",
    val isMemoryExpanded: Boolean = true,
    val cursorPosition: Int = -1 // -1 means at the end of expression
) {
    fun effectiveCursorPosition(): Int {
        return if (cursorPosition in 0..expression.length) cursorPosition else expression.length
    }
}

data class MemoryItem(
    val id: Long = System.currentTimeMillis(),
    val expression: String,
    val formattedValue: String,
    val isAddition: Boolean, // true for M+, false for M-
    val runningTotalFormatted: String
)

data class NotebookPage(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ScreenDestination {
    CALCULATOR,
    CASH_COUNTER,
    AMOUNT_TO_WORDS,
    AGE_CALCULATOR,
    UNIT_CONVERTER,
    NOTEBOOK,
    HISTORY,
    SETTINGS,
    ABOUT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.calculatorDao()
    private val settingsRepo = SettingsRepository(application)
    private val cloudRepo = com.example.data.firebase.NotebookCloudRepository(application)

    // Cloud user info & sync state
    val currentUserEmail: String? get() = cloudRepo.getCurrentUserEmail()
    val currentUserName: String? get() = cloudRepo.getCurrentUserName()
    val isUserSignedIn: Boolean get() = cloudRepo.getCurrentUserId() != null

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow<String?>(null)
    val cloudSyncMessage: StateFlow<String?> = _cloudSyncMessage.asStateFlow()

    val userSettings: StateFlow<UserSettings> = settingsRepo.settings

    // Navigation Drawer / Screen Destination - restored from saved preferences
    private val initialScreen = runCatching {
        ScreenDestination.valueOf(settingsRepo.getLastScreen())
    }.getOrDefault(ScreenDestination.CALCULATOR)
    private val _currentScreen = MutableStateFlow(initialScreen)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Multi-page Calculator State
    private val _pages = MutableStateFlow<List<CalculatorPage>>(
        listOf(CalculatorPage(id = 1, title = "1", expression = "", isBodmas = true))
    )
    val pages: StateFlow<List<CalculatorPage>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(settingsRepo.getLastCalculatorPage())
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // History Search Query
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val historyList: StateFlow<List<CalculationHistoryEntity>> =
        combine(_historySearchQuery, dao.getAllHistory()) { query, list ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.expression.contains(query, ignoreCase = true) ||
                            it.result.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cash Note Counter State
    val noteDenominations = listOf(2000, 500, 200, 100, 50, 20, 10, 5, 2, 1)
    private val _noteCounts = MutableStateFlow(noteDenominations.associateWith { 0 })
    val noteCounts: StateFlow<Map<Int, Int>> = _noteCounts.asStateFlow()

    // Amount to Words State
    private val _amountInput = MutableStateFlow("125500")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    // Age Calculator State
    private val _dobDate = MutableStateFlow<LocalDate>(LocalDate.now().minusYears(25))
    val dobDate: StateFlow<LocalDate> = _dobDate.asStateFlow()

    private val _targetDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val targetDate: StateFlow<LocalDate> = _targetDate.asStateFlow()

    // Unit Converter State
    private val _selectedUnitCategory = MutableStateFlow(UnitConverterEngine.Category.LENGTH)
    val selectedUnitCategory: StateFlow<UnitConverterEngine.Category> = _selectedUnitCategory.asStateFlow()

    private val _fromUnit = MutableStateFlow("m")
    val fromUnit: StateFlow<String> = _fromUnit.asStateFlow()

    private val _toUnit = MutableStateFlow("ft")
    val toUnit: StateFlow<String> = _toUnit.asStateFlow()

    private val _converterInput = MutableStateFlow("1")
    val converterInput: StateFlow<String> = _converterInput.asStateFlow()

    // Notebook State
    private val _notebookPages = MutableStateFlow<List<NotebookPage>>(
        listOf(NotebookPage(id = 1, title = "Note 1", content = ""))
    )
    val notebookPages: StateFlow<List<NotebookPage>> = _notebookPages.asStateFlow()

    private val _currentNotebookPageIndex = MutableStateFlow(settingsRepo.getLastNotebookPage())
    val currentNotebookPageIndex: StateFlow<Int> = _currentNotebookPageIndex.asStateFlow()

    init {
        loadSavedPages()
        loadSavedNotebookPages()
    }

    private fun encodeMemoryItems(items: List<MemoryItem>): String {
        return items.joinToString(";;;") { item ->
            "${item.id}|||${item.expression}|||${item.formattedValue}|||${item.isAddition}|||${item.runningTotalFormatted}"
        }
    }

    private fun decodeMemoryItems(encoded: String): List<MemoryItem> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";;;").mapNotNull { entry ->
            val parts = entry.split("|||")
            if (parts.size >= 5) {
                MemoryItem(
                    id = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                    expression = parts[1],
                    formattedValue = parts[2],
                    isAddition = parts[3].toBooleanStrictOrNull() ?: true,
                    runningTotalFormatted = parts[4]
                )
            } else null
        }
    }

    private fun loadSavedPages() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedEntities = dao.getAllPagesList()
            if (savedEntities.isNotEmpty()) {
                val loadedPages = savedEntities.mapIndexed { index, entity ->
                    val preview = if (entity.expression.isNotBlank()) {
                        when (val res = CalculatorEngine.evaluate(entity.expression, entity.isBodmas)) {
                            is CalculatorEngine.CalculationResult.Success -> res.formatted
                            else -> ""
                        }
                    } else ""
                    CalculatorPage(
                        id = entity.id,
                        title = (index + 1).toString(),
                        expression = entity.expression,
                        previewResult = preview,
                        finalResult = entity.result,
                        isBodmas = entity.isBodmas,
                        memoryItems = decodeMemoryItems(entity.memoryData),
                        memoryTotalFormatted = entity.memoryTotal,
                        isMemoryExpanded = true
                    )
                }
                _pages.value = loadedPages
                val savedPageIndex = settingsRepo.getLastCalculatorPage()
                _currentPageIndex.value = savedPageIndex.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0))
            }
        }
    }

    private fun savePagesToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _pages.value
            val entities = currentList.mapIndexed { index, page ->
                CalculatorPageEntity(
                    id = if (page.id > 0) page.id else 0,
                    pageIndex = index,
                    title = page.title,
                    expression = page.expression,
                    result = page.finalResult,
                    isBodmas = page.isBodmas,
                    memoryData = encodeMemoryItems(page.memoryItems),
                    memoryTotal = page.memoryTotalFormatted
                )
            }
            dao.clearAllPages()
            dao.insertPages(entities)
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
        settingsRepo.setLastScreen(screen.name)
    }

    // --- Calculator Page Actions ---
    fun selectPageIndex(index: Int) {
        if (index in _pages.value.indices) {
            _currentPageIndex.value = index
            settingsRepo.setLastCalculatorPage(index)
        }
    }

    fun addPage() {
        performHapticFeedback()
        val currentList = _pages.value.toMutableList()
        val newIndex = currentList.size + 1
        val isBodmasDefault = userSettings.value.defaultBodmas
        val newPage = CalculatorPage(
            id = System.currentTimeMillis(),
            title = newIndex.toString(),
            expression = "",
            isBodmas = isBodmasDefault
        )
        currentList.add(newPage)
        _pages.value = currentList
        val targetIdx = currentList.size - 1
        _currentPageIndex.value = targetIdx
        settingsRepo.setLastCalculatorPage(targetIdx)
        savePagesToDb()
    }

    fun removeCurrentPage() {
        performHapticFeedback()
        val currentList = _pages.value.toMutableList()
        if (currentList.size <= 1) {
            // If only one page left, just clear it
            clearCurrentPage()
            return
        }
        val currentIndex = _currentPageIndex.value
        currentList.removeAt(currentIndex)

        // Rename titles 1..N
        val reindexed = currentList.mapIndexed { idx, p ->
            p.copy(title = (idx + 1).toString())
        }
        _pages.value = reindexed

        val nextIndex = if (currentIndex >= reindexed.size) reindexed.size - 1 else currentIndex
        _currentPageIndex.value = nextIndex
        settingsRepo.setLastCalculatorPage(nextIndex)
        savePagesToDb()
    }

    fun toggleCurrentPageBodmas() {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]
        val newBodmas = !curPage.isBodmas
        val newPreview = calculatePreview(curPage.expression, newBodmas)
        list[index] = curPage.copy(isBodmas = newBodmas, previewResult = newPreview)
        _pages.value = list
        savePagesToDb()
    }

    fun inputCalculatorChar(char: String) {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]

        var newExpr: String
        var newCursorPos: Int
        val curPos = curPage.effectiveCursorPosition()

        // If a final result was already calculated and user types a digit or bracket, start fresh
        if (curPage.finalResult.isNotBlank() && (char.all { it.isDigit() } || char == "(")) {
            newExpr = char
            newCursorPos = char.length
        } else if (curPage.finalResult.isNotBlank() && isOperator(char)) {
            // Continue with previous result
            val base = curPage.finalResult.replace(",", "")
            newExpr = base + char
            newCursorPos = newExpr.length
        } else {
            val left = curPage.expression.substring(0, curPos)
            val right = curPage.expression.substring(curPos)

            // Prevent consecutive duplicate operators at cursor insertion point
            if (isOperator(char) && left.isNotEmpty() && isOperator(left.takeLast(1))) {
                newExpr = left.dropLast(1) + char + right
                newCursorPos = curPos // replaced previous operator
            } else {
                newExpr = left + char + right
                newCursorPos = curPos + char.length
            }
        }

        val newPreview = calculatePreview(newExpr, curPage.isBodmas)
        list[index] = curPage.copy(
            expression = newExpr,
            cursorPosition = newCursorPos,
            previewResult = newPreview,
            finalResult = ""
        )
        _pages.value = list
        savePagesToDb()
    }

    private fun isOperator(s: String): Boolean {
        return s in listOf("+", "−", "-", "×", "*", "÷", "/", "^", "%")
    }

    fun sanitizePastedText(raw: String): String {
        var text = raw.trim()
        if (text.contains("=")) {
            val parts = text.split("=")
            val candidate = parts.last().trim()
            if (candidate.isNotBlank() && candidate.any { it.isDigit() }) {
                text = candidate
            } else {
                text = parts.first().trim()
            }
        }
        text = text.replace(",", "")
            .replace("₹", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("INR", "", ignoreCase = true)
            .replace(" ", "")
            .replace("-", "−")
            .replace("*", "×")
            .replace("/", "÷")

        val allowed = "0123456789.+-−×*÷/%"
        return text.filter { it in allowed }
    }

    fun pasteToCurrentPage(rawText: String): Boolean {
        val clean = sanitizePastedText(rawText)
        if (clean.isBlank()) return false

        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]
        val curPos = curPage.effectiveCursorPosition()

        val newExpr: String
        val newCursorPos: Int
        if (curPage.finalResult.isNotBlank() || curPage.expression.isBlank()) {
            newExpr = clean
            newCursorPos = clean.length
        } else {
            val left = curPage.expression.substring(0, curPos)
            val right = curPage.expression.substring(curPos)
            newExpr = left + clean + right
            newCursorPos = curPos + clean.length
        }

        val newPreview = calculatePreview(newExpr, curPage.isBodmas)
        list[index] = curPage.copy(
            expression = newExpr,
            cursorPosition = newCursorPos,
            previewResult = newPreview,
            finalResult = ""
        )
        _pages.value = list
        savePagesToDb()
        return true
    }

    fun copyAndPasteToNextPage(rawText: String): Int {
        val clean = sanitizePastedText(rawText)
        if (clean.isBlank()) return -1

        performHapticFeedback()
        val currentList = _pages.value.toMutableList()
        val currentIndex = _currentPageIndex.value
        val targetIndex = currentIndex + 1

        if (targetIndex < currentList.size) {
            val targetPage = currentList[targetIndex]
            val newPreview = calculatePreview(clean, targetPage.isBodmas)
            currentList[targetIndex] = targetPage.copy(
                expression = clean,
                cursorPosition = clean.length,
                previewResult = newPreview,
                finalResult = ""
            )
            _pages.value = currentList
            _currentPageIndex.value = targetIndex
        } else {
            val isBodmasDefault = userSettings.value.defaultBodmas
            val newPage = CalculatorPage(
                id = System.currentTimeMillis(),
                title = (currentList.size + 1).toString(),
                expression = clean,
                cursorPosition = clean.length,
                previewResult = calculatePreview(clean, isBodmasDefault),
                isBodmas = isBodmasDefault
            )
            currentList.add(newPage)
            _pages.value = currentList
            _currentPageIndex.value = currentList.size - 1
        }
        savePagesToDb()
        return _currentPageIndex.value + 1
    }

    fun inputBackspace() {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]

        if (curPage.finalResult.isNotBlank()) {
            list[index] = curPage.copy(
                finalResult = "",
                cursorPosition = curPage.expression.length
            )
            _pages.value = list
            savePagesToDb()
            return
        }

        val curPos = curPage.effectiveCursorPosition()
        if (curPos > 0 && curPage.expression.isNotEmpty()) {
            val left = curPage.expression.substring(0, curPos - 1)
            val right = curPage.expression.substring(curPos)
            val newExpr = left + right
            val newCursorPos = curPos - 1
            val newPreview = calculatePreview(newExpr, curPage.isBodmas)
            list[index] = curPage.copy(
                expression = newExpr,
                cursorPosition = newCursorPos,
                previewResult = newPreview,
                finalResult = ""
            )
            _pages.value = list
            savePagesToDb()
        }
    }

    fun moveCursorLeft(pageIndex: Int = _currentPageIndex.value) {
        performHapticFeedback()
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return
        val curPage = list[pageIndex]
        val curPos = curPage.effectiveCursorPosition()
        val newPos = (curPos - 1).coerceAtLeast(0)
        list[pageIndex] = curPage.copy(cursorPosition = newPos, finalResult = "")
        _pages.value = list
    }

    fun moveCursorRight(pageIndex: Int = _currentPageIndex.value) {
        performHapticFeedback()
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return
        val curPage = list[pageIndex]
        val curPos = curPage.effectiveCursorPosition()
        val newPos = (curPos + 1).coerceAtMost(curPage.expression.length)
        list[pageIndex] = curPage.copy(cursorPosition = newPos, finalResult = "")
        _pages.value = list
    }

    fun setCursorPosition(position: Int, pageIndex: Int = _currentPageIndex.value) {
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return
        val curPage = list[pageIndex]
        val clamped = position.coerceIn(0, curPage.expression.length)
        list[pageIndex] = curPage.copy(cursorPosition = clamped, finalResult = "")
        _pages.value = list
    }

    fun clearCurrentPage() {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]
        list[index] = curPage.copy(
            expression = "",
            cursorPosition = 0,
            previewResult = "",
            finalResult = ""
        )
        _pages.value = list
        savePagesToDb()
    }

    fun evaluateCurrentPage() {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]

        if (curPage.expression.isBlank()) return

        when (val res = CalculatorEngine.evaluate(curPage.expression, curPage.isBodmas)) {
            is CalculatorEngine.CalculationResult.Success -> {
                val formatted = res.formatted
                list[index] = curPage.copy(
                    finalResult = formatted,
                    cursorPosition = curPage.expression.length,
                    previewResult = ""
                )
                _pages.value = list
                savePagesToDb()

                // Save to History database if autoSave is true
                if (userSettings.value.autoSaveHistory) {
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertHistory(
                            CalculationHistoryEntity(
                                expression = curPage.expression,
                                result = formatted,
                                isBodmas = curPage.isBodmas,
                                pageIndex = index
                            )
                        )
                    }
                }
            }
            is CalculatorEngine.CalculationResult.Error -> {
                list[index] = curPage.copy(
                    finalResult = res.message,
                    previewResult = ""
                )
                _pages.value = list
            }
        }
    }

    private fun calculatePreview(expr: String, isBodmas: Boolean): String {
        if (expr.isBlank() || isOperator(expr.takeLast(1))) return ""
        return when (val res = CalculatorEngine.evaluate(expr, isBodmas)) {
            is CalculatorEngine.CalculationResult.Success -> res.formatted
            else -> ""
        }
    }

    fun reuseHistoryItem(item: CalculationHistoryEntity) {
        performHapticFeedback()
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        val curPage = list[index]
        val newPreview = calculatePreview(item.result.replace(",", ""), curPage.isBodmas)
        list[index] = curPage.copy(
            expression = item.result.replace(",", ""),
            previewResult = newPreview,
            finalResult = ""
        )
        _pages.value = list
        _currentScreen.value = ScreenDestination.CALCULATOR
        savePagesToDb()
    }

    // --- Memory (M+, M-, MR, MC) Actions ---
    fun toggleMemoryExpanded(pageIndex: Int = _currentPageIndex.value) {
        val list = _pages.value.toMutableList()
        if (pageIndex in list.indices) {
            val curPage = list[pageIndex]
            list[pageIndex] = curPage.copy(isMemoryExpanded = !curPage.isMemoryExpanded)
            _pages.value = list
        }
    }

    fun memoryAdd(pageIndex: Int = _currentPageIndex.value): MemoryItem? {
        performHapticFeedback()
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return null
        val curPage = list[pageIndex]

        // Value to add: evaluate finalResult if present, or expression
        val evalResult = when {
            curPage.finalResult.isNotBlank() -> CalculatorEngine.evaluate(curPage.finalResult, curPage.isBodmas)
            curPage.expression.isNotBlank() -> CalculatorEngine.evaluate(curPage.expression, curPage.isBodmas)
            else -> null
        }

        if (evalResult is CalculatorEngine.CalculationResult.Success) {
            val stepVal = evalResult.value
            val formattedStep = evalResult.formatted

            // Calculate current total for this page
            val curTotal = curPage.memoryItems.fold(java.math.BigDecimal.ZERO) { acc, itm ->
                val clean = itm.formattedValue.replace(",", "")
                val bd = try { java.math.BigDecimal(clean) } catch (e: Exception) { java.math.BigDecimal.ZERO }
                if (itm.isAddition) acc.add(bd) else acc.subtract(bd)
            }
            val newTotal = curTotal.add(stepVal)
            val formattedTotal = CalculatorEngine.formatResult(newTotal)

            val exprLabel = if (curPage.expression.isNotBlank()) curPage.expression else formattedStep
            val newItem = MemoryItem(
                id = System.currentTimeMillis(),
                expression = exprLabel,
                formattedValue = formattedStep,
                isAddition = true,
                runningTotalFormatted = formattedTotal
            )

            list[pageIndex] = curPage.copy(
                memoryItems = curPage.memoryItems + newItem,
                memoryTotalFormatted = formattedTotal,
                isMemoryExpanded = true,
                finalResult = if (curPage.finalResult.isBlank()) formattedStep else curPage.finalResult,
                previewResult = if (curPage.finalResult.isBlank()) "" else curPage.previewResult
            )
            _pages.value = list
            savePagesToDb()
            return newItem
        }
        return null
    }

    fun memorySubtract(pageIndex: Int = _currentPageIndex.value): MemoryItem? {
        performHapticFeedback()
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return null
        val curPage = list[pageIndex]

        // Value to subtract: evaluate finalResult if present, or expression
        val evalResult = when {
            curPage.finalResult.isNotBlank() -> CalculatorEngine.evaluate(curPage.finalResult, curPage.isBodmas)
            curPage.expression.isNotBlank() -> CalculatorEngine.evaluate(curPage.expression, curPage.isBodmas)
            else -> null
        }

        if (evalResult is CalculatorEngine.CalculationResult.Success) {
            val stepVal = evalResult.value
            val formattedStep = evalResult.formatted

            // Calculate current total for this page
            val curTotal = curPage.memoryItems.fold(java.math.BigDecimal.ZERO) { acc, itm ->
                val clean = itm.formattedValue.replace(",", "")
                val bd = try { java.math.BigDecimal(clean) } catch (e: Exception) { java.math.BigDecimal.ZERO }
                if (itm.isAddition) acc.add(bd) else acc.subtract(bd)
            }
            val newTotal = curTotal.subtract(stepVal)
            val formattedTotal = CalculatorEngine.formatResult(newTotal)

            val exprLabel = if (curPage.expression.isNotBlank()) curPage.expression else formattedStep
            val newItem = MemoryItem(
                id = System.currentTimeMillis(),
                expression = exprLabel,
                formattedValue = formattedStep,
                isAddition = false,
                runningTotalFormatted = formattedTotal
            )

            list[pageIndex] = curPage.copy(
                memoryItems = curPage.memoryItems + newItem,
                memoryTotalFormatted = formattedTotal,
                isMemoryExpanded = true,
                finalResult = if (curPage.finalResult.isBlank()) formattedStep else curPage.finalResult,
                previewResult = if (curPage.finalResult.isBlank()) "" else curPage.previewResult
            )
            _pages.value = list
            savePagesToDb()
            return newItem
        }
        return null
    }

    fun memoryRecall(pageIndex: Int = _currentPageIndex.value): String {
        performHapticFeedback()
        val list = _pages.value
        if (pageIndex !in list.indices) return ""
        val curPage = list[pageIndex]
        val totalStr = curPage.memoryTotalFormatted
        if (totalStr.isNotBlank()) {
            pasteToCurrentPage(totalStr)
        }
        return totalStr
    }

    fun memoryClear(pageIndex: Int = _currentPageIndex.value) {
        performHapticFeedback()
        val list = _pages.value.toMutableList()
        if (pageIndex !in list.indices) return
        val curPage = list[pageIndex]
        list[pageIndex] = curPage.copy(
            memoryItems = emptyList(),
            memoryTotalFormatted = ""
        )
        _pages.value = list
        savePagesToDb()
    }

    // --- History Actions ---
    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun deleteHistoryItem(id: Long) {
        performHapticFeedback()
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHistoryById(id)
        }
    }

    fun clearAllHistory() {
        performHapticFeedback()
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllHistory()
        }
    }

    // --- Cash Counter Actions ---
    fun updateNoteCount(denomination: Int, count: Int) {
        val safeCount = count.coerceAtLeast(0)
        val current = _noteCounts.value.toMutableMap()
        current[denomination] = safeCount
        _noteCounts.value = current
    }

    fun incrementNoteCount(denomination: Int, delta: Int) {
        performHapticFeedback()
        val current = _noteCounts.value.toMutableMap()
        val curVal = current[denomination] ?: 0
        current[denomination] = (curVal + delta).coerceAtLeast(0)
        _noteCounts.value = current
    }

    fun resetNoteCounts() {
        performHapticFeedback()
        _noteCounts.value = noteDenominations.associateWith { 0 }
    }

    // --- Amount to Words Actions ---
    fun setAmountInput(amount: String) {
        _amountInput.value = amount
    }

    fun addAmountPreset(addAmount: Long) {
        performHapticFeedback()
        val current = runCatching { _amountInput.value.replace(",", "").toLong() }.getOrDefault(0L)
        val next = current + addAmount
        _amountInput.value = next.toString()
    }

    // --- Age Calculator Actions ---
    fun setDobDate(date: LocalDate) {
        _dobDate.value = date
    }

    fun setTargetDate(date: LocalDate) {
        _targetDate.value = date
    }

    // --- Unit Converter Actions ---
    fun selectUnitCategory(category: UnitConverterEngine.Category) {
        performHapticFeedback()
        _selectedUnitCategory.value = category
        val units = UnitConverterEngine.getUnitsForCategory(category)
        if (units.isNotEmpty()) {
            _fromUnit.value = units[0].id
            _toUnit.value = units.getOrNull(1)?.id ?: units[0].id
        }
    }

    fun setFromUnit(unitId: String) {
        _fromUnit.value = unitId
    }

    fun setToUnit(unitId: String) {
        _toUnit.value = unitId
    }

    fun swapUnits() {
        performHapticFeedback()
        val temp = _fromUnit.value
        _fromUnit.value = _toUnit.value
        _toUnit.value = temp
    }

    fun setConverterInput(input: String) {
        _converterInput.value = input
    }

    // --- Settings Actions ---
    fun updateTheme(theme: AppTheme) {
        settingsRepo.updateTheme(theme)
    }

    fun updateLanguage(language: AppLanguage) {
        settingsRepo.updateLanguage(language)
    }

    fun toggleLanguage() {
        settingsRepo.toggleLanguage()
    }

    fun updateDefaultBodmas(enabled: Boolean) {
        settingsRepo.updateDefaultBodmas(enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        settingsRepo.updateHapticFeedback(enabled)
    }

    fun updateDecimalPrecision(precision: Int) {
        settingsRepo.updateDecimalPrecision(precision)
    }

    fun updateAutoSaveHistory(enabled: Boolean) {
        settingsRepo.updateAutoSaveHistory(enabled)
    }

    // Haptic feedback trigger
    private fun performHapticFeedback() {
        if (!userSettings.value.hapticFeedback) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (_: Exception) {
        }
    }

    // --- Notebook Persistence & Actions ---
    private fun loadSavedNotebookPages() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedEntities = dao.getAllNotebookPagesList()
            if (savedEntities.isNotEmpty()) {
                val loaded = savedEntities.mapIndexed { index, entity ->
                    NotebookPage(
                        id = entity.id,
                        title = entity.title.ifBlank { "Note ${index + 1}" },
                        content = entity.content,
                        updatedAt = entity.updatedAt
                    )
                }
                _notebookPages.value = loaded
                val savedNotebookIndex = settingsRepo.getLastNotebookPage()
                _currentNotebookPageIndex.value = savedNotebookIndex.coerceIn(0, (loaded.size - 1).coerceAtLeast(0))
            }
        }
    }

    private fun saveNotebookPagesToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _notebookPages.value
            val entities = currentList.mapIndexed { index, page ->
                NotebookPageEntity(
                    id = if (page.id > 0) page.id else 0,
                    pageIndex = index,
                    title = page.title,
                    content = page.content,
                    updatedAt = page.updatedAt
                )
            }
            dao.clearAllNotebookPages()
            dao.insertNotebookPages(entities)
        }
    }

    fun selectNotebookPage(index: Int) {
        if (index in _notebookPages.value.indices) {
            _currentNotebookPageIndex.value = index
            settingsRepo.setLastNotebookPage(index)
        }
    }

    fun updateNotebookPageContent(pageIndex: Int, newContent: String) {
        val list = _notebookPages.value.toMutableList()
        if (pageIndex in list.indices) {
            val updated = list[pageIndex].copy(
                content = newContent,
                updatedAt = System.currentTimeMillis()
            )
            list[pageIndex] = updated
            _notebookPages.value = list
            saveNotebookPagesToDb()
            if (isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepo.savePage(updated)
                }
            }
        }
    }

    fun updateNotebookPageTitle(pageIndex: Int, newTitle: String) {
        val list = _notebookPages.value.toMutableList()
        if (pageIndex in list.indices) {
            val updated = list[pageIndex].copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis()
            )
            list[pageIndex] = updated
            _notebookPages.value = list
            saveNotebookPagesToDb()
            if (isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepo.savePage(updated)
                }
            }
        }
    }

    fun addNotebookPage(): Int {
        performHapticFeedback()
        val list = _notebookPages.value.toMutableList()
        val newIndex = list.size
        val newPage = NotebookPage(
            id = System.currentTimeMillis(),
            title = "Note ${newIndex + 1}",
            content = "",
            updatedAt = System.currentTimeMillis()
        )
        list.add(newPage)
        _notebookPages.value = list
        _currentNotebookPageIndex.value = newIndex
        settingsRepo.setLastNotebookPage(newIndex)
        saveNotebookPagesToDb()
        if (isUserSignedIn) {
            viewModelScope.launch(Dispatchers.IO) {
                cloudRepo.savePage(newPage)
            }
        }
        return newIndex
    }

    fun deleteNotebookPage(pageIndex: Int) {
        performHapticFeedback()
        val list = _notebookPages.value.toMutableList()
        if (list.size <= 1) {
            // If only 1 page left, reset its content
            val resetPage = list[0].copy(content = "", title = "Note 1", updatedAt = System.currentTimeMillis())
            list[0] = resetPage
            _notebookPages.value = list
            saveNotebookPagesToDb()
            if (isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepo.savePage(resetPage)
                }
            }
            return
        }
        if (pageIndex in list.indices) {
            val removed = list.removeAt(pageIndex)
            val reindexed = list.mapIndexed { idx, page ->
                val fallbackTitle = if (page.title.startsWith("Note ")) "Note ${idx + 1}" else page.title
                page.copy(title = fallbackTitle)
            }
            _notebookPages.value = reindexed
            val nextIdx = if (_currentNotebookPageIndex.value >= reindexed.size) reindexed.size - 1 else _currentNotebookPageIndex.value
            _currentNotebookPageIndex.value = nextIdx
            settingsRepo.setLastNotebookPage(nextIdx)
            saveNotebookPagesToDb()
            if (isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepo.deletePage(removed.id)
                }
            }
        }
    }

    fun clearNotebookPage(pageIndex: Int) {
        performHapticFeedback()
        val list = _notebookPages.value.toMutableList()
        if (pageIndex in list.indices) {
            val updated = list[pageIndex].copy(content = "", updatedAt = System.currentTimeMillis())
            list[pageIndex] = updated
            _notebookPages.value = list
            saveNotebookPagesToDb()
            if (isUserSignedIn) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepo.savePage(updated)
                }
            }
        }
    }

    /**
     * Backup and sync all current notebook pages to cloud Firestore.
     */
    fun syncNotebookToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!isUserSignedIn) {
            onComplete(false, "Please sign in with Google first")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isCloudSyncing.value = true
            try {
                cloudRepo.syncAllPages(_notebookPages.value, onError = { errorMsg ->
                    _cloudSyncMessage.value = errorMsg
                })
                _cloudSyncMessage.value = "Cloud backup complete"
                launch(Dispatchers.Main) {
                    onComplete(true, "Cloud backup successful!")
                }
            } catch (e: Exception) {
                _cloudSyncMessage.value = e.localizedMessage
                launch(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Sync error")
                }
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    /**
     * Restore notebook pages from Cloud Firestore to local storage.
     */
    fun restoreNotebookFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!isUserSignedIn) {
            onComplete(false, "Please sign in with Google first")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isCloudSyncing.value = true
            try {
                // Read from cloud repository
                cloudRepo.observeNotebookPages().collect { cloudPages ->
                    if (cloudPages.isNotEmpty()) {
                        _notebookPages.value = cloudPages
                        _currentNotebookPageIndex.value = 0
                        settingsRepo.setLastNotebookPage(0)
                        saveNotebookPagesToDb()
                        launch(Dispatchers.Main) {
                            onComplete(true, "${cloudPages.size} pages restored from Cloud!")
                        }
                    } else {
                        // If no cloud pages exist, sync local ones to cloud
                        cloudRepo.syncAllPages(_notebookPages.value)
                        launch(Dispatchers.Main) {
                            onComplete(true, "Backed up to cloud")
                        }
                    }
                    _isCloudSyncing.value = false
                    return@collect // Single fetch
                }
            } catch (e: Exception) {
                _isCloudSyncing.value = false
                launch(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Failed to restore")
                }
            }
        }
    }

    /**
     * Call when user successfully signs in with Google.
     */
    fun onUserSignedIn(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // First check if cloud has pages; if empty, backup current local pages
            try {
                cloudRepo.observeNotebookPages().collect { cloudPages ->
                    if (cloudPages.isNotEmpty()) {
                        _notebookPages.value = cloudPages
                        saveNotebookPagesToDb()
                    } else {
                        cloudRepo.syncAllPages(_notebookPages.value)
                    }
                    launch(Dispatchers.Main) {
                        onComplete()
                    }
                    return@collect
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
