package com.example.ui.screens.calculator

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppLanguage
import com.example.ui.CalculatorPage
import com.example.ui.MainViewModel
import com.example.ui.MemoryItem
import com.example.ui.ScreenDestination
import com.example.engine.CalculatorEngine
import com.example.ui.theme.CalcAccentAmber
import com.example.ui.theme.CalcAccentGreen
import com.example.ui.theme.CalcKeyActionDark
import com.example.ui.theme.CalcKeyActionLight
import com.example.ui.theme.CalcKeyActionTextDark
import com.example.ui.theme.CalcKeyActionTextLight
import com.example.ui.theme.CalcKeyEqualsDark
import com.example.ui.theme.CalcKeyEqualsLight
import com.example.ui.theme.CalcKeyNumberDark
import com.example.ui.theme.CalcKeyNumberLight
import com.example.ui.theme.CalcKeyNumberTextDark
import com.example.ui.theme.CalcKeyNumberTextLight
import com.example.ui.theme.CalcKeyOperatorDark
import com.example.ui.theme.CalcKeyOperatorLight
import com.example.ui.theme.CalcKeyOperatorTextDark
import com.example.ui.theme.CalcKeyOperatorTextLight
import com.example.util.Localization
import com.example.util.Localization.StringKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val language = settings.language

    val pagerState = rememberPagerState(
        initialPage = currentPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pageCount = { pages.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Sync Pager state with ViewModel current page index
    LaunchedEffect(currentPageIndex, pages.size) {
        if (pages.isNotEmpty()) {
            val target = currentPageIndex.coerceIn(0, pages.size - 1)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }
    }

    // Only update ViewModel when settled page changes via user gesture
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage in pages.indices && pagerState.settledPage != currentPageIndex) {
            viewModel.selectPageIndex(pagerState.settledPage)
        }
    }

    val activePage = pages.getOrNull(currentPageIndex) ?: pages.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 0.dp)
                    ) {
                        Text(
                            text = Localization.getString(StringKey.CALCULATOR, language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // BODMAS Toggle Badge / Chip
                    if (activePage != null) {
                        FilterChip(
                            selected = activePage.isBodmas,
                            onClick = { viewModel.toggleCurrentPageBodmas() },
                            label = {
                                Text(
                                    text = if (activePage.isBodmas) "BODMAS ON" else "BODMAS OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("bodmas_toggle_button")
                        )
                    }

                    // Quick Language Toggle
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "ಕನ್ನಡ" else "EN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Quick History Screen
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenDestination.HISTORY) },
                        modifier = Modifier.testTag("history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Multi-Page Navigation Bar ---
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Page indicator pill (e.g. "Page 1 / 5")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "${currentPageIndex + 1} / ${pages.size}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Prev Page button
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(currentPageIndex - 1)
                                    }
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Previous Page",
                                modifier = Modifier.size(14.dp),
                                tint = if (currentPageIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        // Next Page button
                        IconButton(
                            onClick = {
                                if (currentPageIndex < pages.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(currentPageIndex + 1)
                                    }
                                }
                            },
                            enabled = currentPageIndex < pages.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Next Page",
                                modifier = Modifier.size(14.dp),
                                tint = if (currentPageIndex < pages.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Swipeable Page Chips
                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(pages) { index, page ->
                            val isSelected = index == currentPageIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "P${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Action buttons: Paste, Add Page (+), and Delete Page (🗑)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Paste button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val clipText = clipboardManager.getText()?.text
                                    if (clipText.isNullOrBlank()) {
                                        Toast.makeText(
                                            context,
                                            Localization.getString(StringKey.CLIPBOARD_EMPTY, language),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val success = viewModel.pasteToCurrentPage(clipText)
                                        if (success) {
                                            val clean = viewModel.sanitizePastedText(clipText)
                                            val msg = if (language == AppLanguage.KANNADA) {
                                                "ಪೇಸ್ಟ್ ಮಾಡಲಾಗಿದೆ: $clean"
                                            } else {
                                                "Pasted: $clean"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                Localization.getString(StringKey.CLIPBOARD_EMPTY, language),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                .testTag("paste_page_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = Localization.getString(StringKey.PASTE, language),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Add Page
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.addPage()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pages.size)
                                    }
                                }
                                .testTag("add_page_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = Localization.getString(StringKey.ADD_PAGE, language),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Delete Page
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (pages.size > 1) {
                                        showDeleteConfirmDialog = true
                                    } else {
                                        viewModel.clearCurrentPage()
                                        Toast.makeText(
                                            context,
                                            Localization.getString(StringKey.CLEAR_PAGE, language),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .testTag("delete_page_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = Localization.getString(StringKey.DELETE_PAGE, language),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- Multi-Screen Horizontal Pager (Display Area) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIdx ->
                    val page = pages.getOrNull(pageIdx)
                    if (page != null) {
                        CalculationDisplayView(
                            page = page,
                            language = language,
                            onToggleMemoryExpanded = { viewModel.toggleMemoryExpanded(pageIdx) },
                            onMemoryClear = {
                                viewModel.memoryClear(pageIdx)
                                Toast.makeText(
                                    context,
                                    Localization.getString(StringKey.MEMORY_CLEARED_MSG, language),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onMemoryRecall = {
                                val recalled = viewModel.memoryRecall(pageIdx)
                                if (recalled.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        Localization.getString(StringKey.MEMORY_EMPTY_MSG, language),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onSetCursorPosition = { pos -> viewModel.setCursorPosition(pos, pageIdx) },
                            onCopy = { textToCopy ->
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(
                                    context,
                                    Localization.getString(StringKey.COPIED_TO_CLIPBOARD, language),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            // --- Keypad Area ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.62f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                CalculatorKeypad(
                    hasMemory = activePage?.memoryItems?.isNotEmpty() == true,
                    onCharInput = { viewModel.inputCalculatorChar(it) },
                    onBackspace = { viewModel.inputBackspace() },
                    onClear = { viewModel.clearCurrentPage() },
                    onEquals = { viewModel.evaluateCurrentPage() },
                    onMemoryAdd = {
                        val item = viewModel.memoryAdd(currentPageIndex)
                        if (item != null) {
                            val msg = if (language == AppLanguage.KANNADA) {
                                "M+ ಸೇರಿಸಲಾಗಿದೆ: +${item.formattedValue} (ಒಟ್ಟು: ${item.runningTotalFormatted})"
                            } else {
                                "M+ Added: +${item.formattedValue} (Total: ${item.runningTotalFormatted})"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = if (language == AppLanguage.KANNADA) "ಲೆಕ್ಕ ಅಥವಾ ಸಂಖ್ಯೆಯನ್ನು ನಮೂದಿಸಿ" else "Enter a number or calculation first"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMemorySubtract = {
                        val item = viewModel.memorySubtract(currentPageIndex)
                        if (item != null) {
                            val msg = if (language == AppLanguage.KANNADA) {
                                "M- ಕಳೆಯಲಾಗಿದೆ: -${item.formattedValue} (ಒಟ್ಟು: ${item.runningTotalFormatted})"
                            } else {
                                "M- Subtracted: -${item.formattedValue} (Total: ${item.runningTotalFormatted})"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = if (language == AppLanguage.KANNADA) "ಲೆಕ್ಕ ಅಥವಾ ಸಂಖ್ಯೆಯನ್ನು ನಮೂದಿಸಿ" else "Enter a number or calculation first"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMemoryRecall = {
                        val recalled = viewModel.memoryRecall(currentPageIndex)
                        if (recalled.isNotBlank()) {
                            val msg = if (language == AppLanguage.KANNADA) "ಮೆಮೊರಿ ಮರುಪಡೆಯಲಾಗಿದೆ: $recalled" else "Recalled: $recalled"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = Localization.getString(StringKey.MEMORY_EMPTY_MSG, language)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMemoryClear = {
                        viewModel.memoryClear(currentPageIndex)
                        val msg = Localization.getString(StringKey.MEMORY_CLEARED_MSG, language)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Delete Page Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(text = Localization.getString(StringKey.DELETE_PAGE_CONFIRM, language))
            },
            text = {
                Text(text = Localization.getString(StringKey.DELETE_PAGE_DESC, language))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeCurrentPage()
                        showDeleteConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_page")
                ) {
                    Text(
                        text = Localization.getString(StringKey.DELETE, language),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(text = Localization.getString(StringKey.CANCEL, language))
                }
            }
        )
    }
}

@Composable
fun CalculationDisplayView(
    page: CalculatorPage,
    language: AppLanguage,
    onToggleMemoryExpanded: () -> Unit,
    onMemoryClear: () -> Unit,
    onMemoryRecall: () -> Unit,
    onSetCursorPosition: (Int) -> Unit,
    onCopy: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    LaunchedEffect(page.expression, page.finalResult, page.memoryItems.size, page.cursorPosition) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // --- Detailed Memory Calculation Card (Shown when M+, M-, MR are active for THIS page) ---
        if (page.memoryItems.isNotEmpty()) {
            MemoryDetailsCard(
                memoryItems = page.memoryItems,
                memoryTotalFormatted = page.memoryTotalFormatted,
                isExpanded = page.isMemoryExpanded,
                language = language,
                onToggleExpand = onToggleMemoryExpanded,
                onClear = onMemoryClear,
                onRecall = onMemoryRecall
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // --- Expression Line with Interactive Tap-to-Position Cursor and Visual Cursor Marker ---
        val curPos = page.effectiveCursorPosition()
        val isEditing = page.finalResult.isBlank()
        val primaryColor = MaterialTheme.colorScheme.primary
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        // Format expression with Indian commas (e.g. 1,00,000 + 5,000)
        val formattedExpr: String = remember(page.expression) {
            com.example.engine.CalculatorEngine.formatExpressionWithIndianCommas(page.expression)
        }
        val formattedCurPos: Int = remember(page.expression, curPos) {
            com.example.engine.CalculatorEngine.mapRawOffsetToFormatted(page.expression, curPos)
        }

        val annotatedExpression = remember(formattedExpr, formattedCurPos, isEditing, cursorAlpha, primaryColor) {
            buildAnnotatedString {
                if (formattedExpr.isEmpty()) {
                    if (isEditing) {
                        withStyle(SpanStyle(color = primaryColor.copy(alpha = cursorAlpha), fontWeight = FontWeight.ExtraBold)) {
                            append("|")
                        }
                    } else {
                        append("0")
                    }
                } else {
                    val clampedPos = formattedCurPos.coerceIn(0, formattedExpr.length)
                    append(formattedExpr.substring(0, clampedPos))
                    if (isEditing) {
                        withStyle(SpanStyle(color = primaryColor.copy(alpha = cursorAlpha), fontWeight = FontWeight.ExtraBold)) {
                            append("|")
                        }
                    }
                    append(formattedExpr.substring(clampedPos))
                }
            }
        }

        val fontSize = when {
            formattedExpr.length > 24 -> 20.sp
            formattedExpr.length > 16 -> 26.sp
            formattedExpr.length > 10 -> 32.sp
            else -> 38.sp
        }

        Text(
            text = annotatedExpression,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp
            ),
            color = if (page.finalResult.isNotBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.End,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(page.expression, formattedExpr, isEditing, curPos, formattedCurPos) {
                    detectTapGestures(
                        onTap = { offset ->
                            textLayoutResult?.let { layout ->
                                val tappedOffset = layout.getOffsetForPosition(offset)
                                val clampedFormattedPos = formattedCurPos.coerceIn(0, formattedExpr.length)
                                val rawTargetFormatted = if (isEditing && tappedOffset > clampedFormattedPos) {
                                    tappedOffset - 1
                                } else {
                                    tappedOffset
                                }
                                val actualRawPos: Int = com.example.engine.CalculatorEngine.mapFormattedOffsetToRaw(
                                    page.expression,
                                    rawTargetFormatted.coerceIn(0, formattedExpr.length)
                                )
                                onSetCursorPosition(actualRawPos.coerceIn(0, page.expression.length))
                            }
                        },
                        onLongPress = {
                            if (page.expression.isNotBlank()) {
                                onCopy(page.expression)
                            }
                        }
                    )
                }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Final Result or Live Preview Result
        if (page.finalResult.isNotBlank()) {
            Text(
                text = "= ${page.finalResult}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = when {
                        page.finalResult.length > 20 -> 24.sp
                        page.finalResult.length > 14 -> 30.sp
                        else -> 36.sp
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCopy(page.finalResult)
                    }
            )
        } else if (page.previewResult.isNotBlank()) {
            Text(
                text = "= ${page.previewResult}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCopy(page.previewResult)
                    }
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MemoryDetailsCard(
    memoryItems: List<MemoryItem>,
    memoryTotalFormatted: String,
    isExpanded: Boolean,
    language: AppLanguage,
    onToggleExpand: () -> Unit,
    onClear: () -> Unit,
    onRecall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("memory_details_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Header Row: [M] Total, Action Chips (MR, MC, Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "M",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = if (memoryTotalFormatted.isNotBlank()) "= $memoryTotalFormatted" else "= 0",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${memoryItems.size})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MR Recall Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRecall() }
                    ) {
                        Text(
                            text = Localization.getString(StringKey.MEMORY_RECALL, language),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // MC Clear Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClear() }
                    ) {
                        Text(
                            text = Localization.getString(StringKey.MEMORY_CLEAR, language),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Expand/Collapse Icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand or Collapse Memory",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Step Details Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    memoryItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "${idx + 1}. ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.isAddition) Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFFDC2626).copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.isAddition) "M+" else "M-",
                                        color = if (item.isAddition) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = item.expression,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${if (item.isAddition) "+" else "-"}${item.formattedValue} (Σ ${item.runningTotalFormatted})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.isAddition) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorKeypad(
    hasMemory: Boolean,
    onCharInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryClear: () -> Unit
) {
    // 6 Rows Layout with Row 0 (MC, MR, M-, M+) and Big '+' key spanning Rows 4 and 5
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 0 (Memory Row): MC, MR, M-, M+
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton(
                text = "MC",
                type = if (hasMemory) KeyType.MEMORY_ACTIVE else KeyType.MEMORY,
                modifier = Modifier.weight(1f),
                onClick = onMemoryClear,
                testTag = "btn_mc"
            )
            KeyButton(
                text = "MR",
                type = if (hasMemory) KeyType.MEMORY_ACTIVE else KeyType.MEMORY,
                modifier = Modifier.weight(1f),
                onClick = onMemoryRecall,
                testTag = "btn_mr"
            )
            KeyButton(
                text = "M-",
                type = KeyType.MEMORY,
                modifier = Modifier.weight(1f),
                onClick = onMemorySubtract,
                testTag = "btn_m_minus"
            )
            KeyButton(
                text = "M+",
                type = KeyType.MEMORY,
                modifier = Modifier.weight(1f),
                onClick = onMemoryAdd,
                testTag = "btn_m_plus"
            )
        }

        // Row 1: AC, ⌫, %, ÷
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton(
                text = "AC",
                type = KeyType.CLEAR,
                modifier = Modifier.weight(1f),
                onClick = onClear,
                testTag = "btn_clear"
            )
            KeyButton(
                text = "⌫",
                type = KeyType.BACKSPACE,
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
                testTag = "btn_backspace"
            )
            KeyButton(
                text = "%",
                type = KeyType.OPERATOR,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("%") },
                testTag = "btn_percent"
            )
            KeyButton(
                text = "÷",
                type = KeyType.OPERATOR_PRIMARY,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("÷") },
                testTag = "btn_divide"
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton(
                text = "7",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("7") },
                testTag = "btn_7"
            )
            KeyButton(
                text = "8",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("8") },
                testTag = "btn_8"
            )
            KeyButton(
                text = "9",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("9") },
                testTag = "btn_9"
            )
            KeyButton(
                text = "×",
                type = KeyType.OPERATOR_PRIMARY,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("×") },
                testTag = "btn_multiply"
            )
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton(
                text = "4",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("4") },
                testTag = "btn_4"
            )
            KeyButton(
                text = "5",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("5") },
                testTag = "btn_5"
            )
            KeyButton(
                text = "6",
                type = KeyType.NUMBER,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("6") },
                testTag = "btn_6"
            )
            KeyButton(
                text = "−",
                type = KeyType.OPERATOR_PRIMARY,
                modifier = Modifier.weight(1f),
                onClick = { onCharInput("−") },
                testTag = "btn_minus"
            )
        }

        // Rows 4 & 5 Combined:
        // Left column: Sub-Row 4 (1, 2, 3) and Sub-Row 5 (0, ., =)
        // Right column: Big '+' button spanning both rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sub-Row 4: 1, 2, 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyButton(
                        text = "1",
                        type = KeyType.NUMBER,
                        modifier = Modifier.weight(1f),
                        onClick = { onCharInput("1") },
                        testTag = "btn_1"
                    )
                    KeyButton(
                        text = "2",
                        type = KeyType.NUMBER,
                        modifier = Modifier.weight(1f),
                        onClick = { onCharInput("2") },
                        testTag = "btn_2"
                    )
                    KeyButton(
                        text = "3",
                        type = KeyType.NUMBER,
                        modifier = Modifier.weight(1f),
                        onClick = { onCharInput("3") },
                        testTag = "btn_3"
                    )
                }

                // Sub-Row 5: 0, ., =
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyButton(
                        text = "0",
                        type = KeyType.NUMBER,
                        modifier = Modifier.weight(1f),
                        onClick = { onCharInput("0") },
                        testTag = "btn_0"
                    )
                    KeyButton(
                        text = ".",
                        type = KeyType.NUMBER,
                        modifier = Modifier.weight(1f),
                        onClick = { onCharInput(".") },
                        testTag = "btn_dot"
                    )
                    KeyButton(
                        text = "=",
                        type = KeyType.EQUALS,
                        modifier = Modifier.weight(1f),
                        onClick = onEquals,
                        testTag = "btn_equals"
                    )
                }
            }

            // Big '+' Button spanning Rows 4 & 5
            KeyButton(
                text = "+",
                type = KeyType.OPERATOR_PRIMARY,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onCharInput("+") },
                testTag = "btn_plus"
            )
        }
    }
}

enum class KeyType {
    NUMBER,
    OPERATOR,
    OPERATOR_PRIMARY,
    CLEAR,
    BACKSPACE,
    EQUALS,
    MEMORY,
    MEMORY_ACTIVE
}

@Composable
fun KeyButton(
    text: String,
    type: KeyType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val containerColor = when (type) {
        KeyType.NUMBER -> if (isDark) CalcKeyNumberDark else CalcKeyNumberLight
        KeyType.OPERATOR -> if (isDark) CalcKeyOperatorDark else CalcKeyOperatorLight
        KeyType.OPERATOR_PRIMARY -> if (isDark) CalcKeyOperatorDark else CalcKeyOperatorLight
        KeyType.CLEAR -> if (isDark) CalcKeyActionDark else CalcKeyActionLight
        KeyType.BACKSPACE -> if (isDark) CalcKeyOperatorDark else CalcKeyOperatorLight
        KeyType.EQUALS -> MaterialTheme.colorScheme.primary
        KeyType.MEMORY -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        KeyType.MEMORY_ACTIVE -> if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }

    val contentColor = when (type) {
        KeyType.NUMBER -> if (isDark) CalcKeyNumberTextDark else CalcKeyNumberTextLight
        KeyType.OPERATOR -> if (isDark) CalcKeyOperatorTextDark else CalcKeyOperatorTextLight
        KeyType.OPERATOR_PRIMARY -> if (isDark) CalcAccentAmber else Color(0xFFD97706)
        KeyType.CLEAR -> if (isDark) CalcKeyActionTextDark else CalcKeyActionTextLight
        KeyType.BACKSPACE -> if (isDark) CalcKeyOperatorTextDark else CalcKeyOperatorTextLight
        KeyType.EQUALS -> MaterialTheme.colorScheme.onPrimary
        KeyType.MEMORY -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        KeyType.MEMORY_ACTIVE -> MaterialTheme.colorScheme.primary
    }

    val borderColor = when (type) {
        KeyType.EQUALS -> if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        KeyType.CLEAR -> if (isDark) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFDC2626).copy(alpha = 0.4f)
        KeyType.OPERATOR_PRIMARY -> if (isDark) CalcAccentAmber.copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.45f)
        KeyType.OPERATOR -> if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
        KeyType.BACKSPACE -> if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
        KeyType.NUMBER -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
        KeyType.MEMORY -> if (isDark) Color(0xFF334155).copy(alpha = 0.7f) else Color(0xFFCBD5E1)
        KeyType.MEMORY_ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .testTag(testTag),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor),
        tonalElevation = if (type == KeyType.NUMBER) 2.dp else 4.dp,
        shadowElevation = if (type == KeyType.EQUALS) 6.dp else 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (type == KeyType.BACKSPACE) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = text,
                    fontSize = when {
                        text == "AC" -> 20.sp
                        text == "MC" || text == "MR" || text == "M+" || text == "M-" -> 17.sp
                        text == "+" -> 32.sp
                        else -> 24.sp
                    },
                    fontWeight = if (type == KeyType.EQUALS || type == KeyType.OPERATOR_PRIMARY || type == KeyType.CLEAR || type == KeyType.MEMORY_ACTIVE) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}
