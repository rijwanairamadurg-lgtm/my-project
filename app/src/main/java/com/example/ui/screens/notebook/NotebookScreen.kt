package com.example.ui.screens.notebook

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.NotebookPage
import com.example.ui.ScreenDestination
import com.example.util.Localization
import com.example.util.Localization.StringKey
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import com.example.ui.auth.GoogleSignInHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val notebookPages by viewModel.notebookPages.collectAsState()
    val currentNotebookPageIndex by viewModel.currentNotebookPageIndex.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val language = settings.language

    val pagerState = rememberPagerState(
        initialPage = currentNotebookPageIndex.coerceIn(0, (notebookPages.size - 1).coerceAtLeast(0)),
        pageCount = { notebookPages.size }
    )
    val tabListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showKeypad by remember { mutableStateOf(true) }
    var keypadAlpha by remember { mutableFloatStateOf(0.78f) } // Transparent slider / toggle
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }

    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val isUserSignedIn = viewModel.isUserSignedIn
    val currentUserEmail = viewModel.currentUserEmail

    // Sync Pager state with ViewModel current notebook page index
    LaunchedEffect(currentNotebookPageIndex, notebookPages.size) {
        if (notebookPages.isNotEmpty()) {
            val target = currentNotebookPageIndex.coerceIn(0, notebookPages.size - 1)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }
    }

    // Auto-scroll the page tab list to keep the current page visible
    LaunchedEffect(pagerState.currentPage) {
        if (notebookPages.isNotEmpty() && pagerState.currentPage in notebookPages.indices) {
            tabListState.animateScrollToItem(pagerState.currentPage)
        }
    }

    // Only update ViewModel when settled page changes via user gesture
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage in notebookPages.indices && pagerState.settledPage != currentNotebookPageIndex) {
            viewModel.selectNotebookPage(pagerState.settledPage)
        }
    }

    val activePage = notebookPages.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.KANNADA) "ನೋಟ್‌ಬುಕ್ (Notebook)" else "Notebook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("notebook_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Cloud Backup & Sync button (ಮೊಬೈಲ್ ಕಳೆದರೂ ಡೇಟಾ ಸಿಗುವಂತೆ ಕ್ಲೌಡ್ ಬ್ಯಾಕಪ್)
                    IconButton(
                        onClick = { showCloudSyncDialog = true },
                        modifier = Modifier.testTag("cloud_sync_button")
                    ) {
                        if (isCloudSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (isUserSignedIn) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = "Cloud Backup & Restore",
                                tint = if (isUserSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggle transparent custom keypad
                    IconButton(
                        onClick = { showKeypad = !showKeypad },
                        modifier = Modifier.testTag("toggle_keypad_button")
                    ) {
                        Icon(
                            imageVector = if (showKeypad) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = "Toggle Keypad",
                            tint = if (showKeypad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Copy entire page note
                    IconButton(
                        onClick = {
                            val textToCopy = activePage?.content.orEmpty()
                            if (textToCopy.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                val msg = if (language == AppLanguage.KANNADA) "ನೋಟ್ ಕಾಪಿ ಮಾಡಲಾಗಿದೆ!" else "Note copied to clipboard!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                val msg = if (language == AppLanguage.KANNADA) "ನೋಟ್‌ಬುಕ್‌ನಲ್ಲಿ ಯಾವುದೇ ಬರಹವಿಲ್ಲ" else "Note is empty"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("copy_note_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Note")
                    }

                    // Delete current page
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_note_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Page",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B),
                    navigationIconContentColor = Color(0xFF1E293B),
                    actionIconContentColor = Color(0xFF1E293B)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            // --- Multi-Page Tab Bar ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page indicator badge (e.g. 1 / 3)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${notebookPages.size}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Left arrow
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Previous Page",
                            modifier = Modifier.size(13.dp),
                            tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    // Page chip selector list - displaying Note Title (unselected shows 3 letters, selected shows full title)
                    LazyRow(
                        state = tabListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(notebookPages) { idx, page ->
                            val isSelected = idx == pagerState.currentPage
                            val trimmedTitle = page.title.trim()
                            val displayTitle = if (isSelected) {
                                // For open / active page, show full title (up to 16 chars)
                                if (trimmedTitle.isNotBlank()) trimmedTitle.take(16)
                                else (if (language == AppLanguage.KANNADA) "ಪುಟ ${idx + 1}" else "Page ${idx + 1}")
                            } else {
                                // For other pages, show first 3 letters (ಮೂರು ಅಕ್ಷರ) of title as requested by user
                                if (trimmedTitle.isNotBlank()) {
                                    if (trimmedTitle.length > 3) "${trimmedTitle.take(3)}.." else trimmedTitle
                                } else {
                                    "${idx + 1}"
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = if (isSelected) 10.dp else 8.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text(
                                        text = displayTitle,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Right arrow
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < notebookPages.size - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < notebookPages.size - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Next Page",
                            modifier = Modifier.size(13.dp),
                            tint = if (pagerState.currentPage < notebookPages.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Add Page Button (+)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                val newIdx = viewModel.addNotebookPage()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(newIdx)
                                }
                            }
                            .testTag("add_notebook_page_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- Notebook Sheet Content (Rendered with real notebook lined page look) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIdx ->
                    val page = notebookPages.getOrNull(pageIdx)
                    if (page != null) {
                        NotebookSheetView(
                            page = page,
                            pageIndex = pageIdx,
                            language = language,
                            onContentChange = { newContent ->
                                viewModel.updateNotebookPageContent(pageIdx, newContent)
                            },
                            onTitleChange = { newTitle ->
                                viewModel.updateNotebookPageTitle(pageIdx, newTitle)
                            }
                        )
                    }
                }

                // --- Floating Transparent Custom Keypad Overlay ---
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showKeypad,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        TransparentNotebookKeypad(
                            alpha = keypadAlpha,
                            onAlphaChange = { keypadAlpha = it },
                            language = language,
                            onCharInput = { char ->
                                val cur = activePage?.content.orEmpty()
                                viewModel.updateNotebookPageContent(pagerState.currentPage, cur + char)
                            },
                            onBackspace = {
                                val cur = activePage?.content.orEmpty()
                                if (cur.isNotEmpty()) {
                                    viewModel.updateNotebookPageContent(pagerState.currentPage, cur.dropLast(1))
                                }
                            },
                            onNewLine = {
                                val cur = activePage?.content.orEmpty()
                                viewModel.updateNotebookPageContent(pagerState.currentPage, cur + "\n")
                            },
                            onSpace = {
                                val cur = activePage?.content.orEmpty()
                                viewModel.updateNotebookPageContent(pagerState.currentPage, cur + " ")
                            },
                            onClear = {
                                viewModel.clearNotebookPage(pagerState.currentPage)
                            },
                            onHide = { showKeypad = false }
                        )
                    }
                }
            }
        }
    }

    // Cloud Sync & Google Sign-in Dialog (ಮೊಬೈಲ್ ಕಳೆದರೂ ಡೇಟಾ ಸಿಗುವಂತೆ)
    if (showCloudSyncDialog) {
        val credentialManager = remember { CredentialManager.create(context) }
        var isSigningIn by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCloudSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.KANNADA) "ಕ್ಲೌಡ್ ಬ್ಯಾಕಪ್ (Cloud Backup)" else "Cloud Backup & Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isUserSignedIn) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == AppLanguage.KANNADA) "ಲಾಗಿನ್ ಆಗಿರುವ ಖಾತೆ:" else "Connected Google Account:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUserEmail ?: "Google Account",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = if (language == AppLanguage.KANNADA)
                                "ನಿಮ್ಮ ನೋಟ್‌ಬುಕ್ ಪುಟಗಳು Google Cloud ನಲ್ಲಿ ಸುರಕ್ಷಿತವಾಗಿವೆ. ಮೊಬೈಲ್ ಕಳೆದುಹೋದರೂ ಅಥವಾ ಹೊಸ ಮೊಬೈಲ್ ಬದಲಾಯಿಸಿದರೂ ಇದೇ Google ಖಾತೆಯಿಂದ ನಿಮ್ಮ ಎಲ್ಲಾ ನೋಟ್ಸ್ ಮರಳಿ ಸಿಗುತ್ತವೆ!"
                            else
                                "Your notebook pages are backed up to Google Cloud. Even if you lose your phone or switch devices, sign in with this Google account to get all your notes back anytime!",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.KANNADA)
                                "ಮೊಬೈಲ್ ಕಳೆದುಹೋದರೂ ಅಥವಾ ರಿಸ್ಟೋರ್ ಮಾಡಿದರೂ ನೋಟ್‌ಬುಕ್‌ನಲ್ಲಿ ಬರೆದ ಡೇಟಾ ಕಳೆದುಹೋಗದಂತೆ Google Sign-In ಮೂಲಕ ಕ್ಲೌಡ್ ಬ್ಯಾಕಪ್ ಮಾಡಿಕೊಳ್ಳಿ."
                            else
                                "Backup your notes safely to Google Cloud. Even if your phone is lost, replaced, or reset, your data is always safe and recoverable!",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Google Sign-In Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isSigningIn) {
                                    isSigningIn = true
                                    GoogleSignInHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isSigningIn = false
                                            viewModel.onUserSignedIn {
                                                val msg = if (language == AppLanguage.KANNADA)
                                                    "Google ಲಾಗಿನ್ ಯಶಸ್ವಿಯಾಗಿದೆ! ಡೇಟಾ ಬ್ಯಾಕಪ್ ಆಗಿದೆ."
                                                else
                                                    "Google Sign-In successful! Data backed up."
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onAuthError = { errorMsg ->
                                            isSigningIn = false
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        },
                                        scope = coroutineScope,
                                        onAuthCancelled = {
                                            isSigningIn = false
                                        }
                                    )
                                }
                                .testTag("google_sign_in_button")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (language == AppLanguage.KANNADA) "Google ಖಾತೆಯೊಂದಿಗೆ ಸೈನ್-ಇನ್ ಆಗಿ" else "Sign in with Google",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isUserSignedIn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.syncNotebookToCloud { success, msg ->
                                    val toastMsg = if (success) {
                                        if (language == AppLanguage.KANNADA) "ಕ್ಲೌಡ್‌ಗೆ ಯಶಸ್ವಿಯಾಗಿ ಬ್ಯಾಕಪ್ ಮಾಡಲಾಗಿದೆ!" else "Backed up to cloud successfully!"
                                    } else msg
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (language == AppLanguage.KANNADA) "ಈಗಲೇ ಬ್ಯಾಕಪ್ ಮಾಡಿ" else "Backup Now")
                        }
                        TextButton(
                            onClick = {
                                viewModel.restoreNotebookFromCloud { success, msg ->
                                    val toastMsg = if (success) {
                                        if (language == AppLanguage.KANNADA) "ಕ್ಲೌಡ್‌ನಿಂದ ಡೇಟಾ ಮರುಪಡೆಯಲಾಗಿದೆ!" else "Restored from cloud successfully!"
                                    } else msg
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (language == AppLanguage.KANNADA) "ಕ್ಲೌಡ್‌ನಿಂದ ಮರುಪಡೆಯಿರಿ" else "Restore")
                        }
                    }
                } else {
                    TextButton(onClick = { showCloudSyncDialog = false }) {
                        Text(if (language == AppLanguage.KANNADA) "ಮುಚ್ಚಿ" else "Close")
                    }
                }
            },
            dismissButton = {
                if (isUserSignedIn) {
                    TextButton(
                        onClick = {
                            GoogleSignInHelper.signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    showCloudSyncDialog = false
                                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                },
                                scope = coroutineScope
                            )
                        }
                    ) {
                        Text(
                            text = if (language == AppLanguage.KANNADA) "ಸೈನ್-ಔಟ್" else "Sign out",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(if (language == AppLanguage.KANNADA) "ಪುಟವನ್ನು ಅಳಿಸಬೇಕೆ?" else "Delete this note page?")
            },
            text = {
                Text(
                    if (language == AppLanguage.KANNADA)
                        "ಇದು ಪ್ರಸ್ತುತ ಪುಟದಲ್ಲಿ ಬರೆದಿರುವ ವಿಷಯವನ್ನು ಸಂಪೂರ್ಣವಾಗಿ ತೆಗೆದುಹಾಕುತ್ತದೆ."
                    else
                        "This will remove the current notebook page and its notes."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNotebookPage(pagerState.currentPage)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(
                        text = if (language == AppLanguage.KANNADA) "ಅಳಿಸಿ" else "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (language == AppLanguage.KANNADA) "ರದ್ದುಮಾಡಿ" else "Cancel")
                }
            }
        )
    }
}

/**
 * Authentic lined notebook paper look with margin, punched spiral holes, and editable ruled lines.
 */
@Composable
fun NotebookSheetView(
    page: NotebookPage,
    pageIndex: Int,
    language: AppLanguage,
    onContentChange: (String) -> Unit,
    onTitleChange: (String) -> Unit
) {
    // Paper styling colors - Always pure authentic white paper (ಬಿಳಿ ಬಣ್ಣದ ನೋಟ್‌ಬುಕ್ ಥೀಮ್)
    val paperBg = Color.White
    // Crisp, clearly visible notebook ruled line blue (ಅಡ್ಡ ಗೆರೆಗಳು)
    val ruledLineColor = Color(0xFF6DA4E4).copy(alpha = 0.85f)
    // Red margin and double top header lines
    val marginLineColor = Color(0xFFE53935).copy(alpha = 0.70f)
    val headerLineColor = Color(0xFFE53935).copy(alpha = 0.75f)
    val textColor = Color(0xFF0F172A)
    val spiralColor = Color(0xFF94A3B8)

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(page.updatedAt) { dateFormat.format(Date(page.updatedAt)) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val totalCardHeight = maxHeight

        // Notebook Paper Card - Pure White Sheet
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            val scrollState = rememberScrollState()

            // Scrollable Notebook Page Content - lines scroll seamlessly with text!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = totalCardHeight)
                    .verticalScroll(scrollState)
                    .drawBehind {
                        val w = size.width
                        val h = size.height

                        // 1. Left spiral punched holes
                        val holeRadius = 3.5.dp.toPx()
                        val holeSpacing = 28.dp.toPx()
                        var curHoleY = 20.dp.toPx()
                        while (curHoleY < h) {
                            drawCircle(
                                color = spiralColor.copy(alpha = 0.5f),
                                radius = holeRadius,
                                center = Offset(16.dp.toPx(), curHoleY)
                            )
                            curHoleY += holeSpacing
                        }

                        // 2. Left red vertical margin line (double line in Indian notebooks)
                        val marginX1 = 44.dp.toPx()
                        val marginX2 = 47.dp.toPx()
                        drawLine(
                            color = marginLineColor,
                            start = Offset(marginX1, 0f),
                            end = Offset(marginX1, h),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = marginLineColor.copy(alpha = 0.5f),
                            start = Offset(marginX2, 0f),
                            end = Offset(marginX2, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 3. Top double red horizontal header line under Title/Date box
                        val headerY1 = 82.dp.toPx()
                        val headerY2 = 86.dp.toPx()
                        drawLine(
                            color = headerLineColor,
                            start = Offset(0f, headerY1),
                            end = Offset(w, headerY1),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = headerLineColor.copy(alpha = 0.6f),
                            start = Offset(0f, headerY2),
                            end = Offset(w, headerY2),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 4. Horizontal ruled lines (ಅಡ್ಡ ಗೆರೆಗಳು) across the entire body
                        val lineSpacing = 36.dp.toPx()
                        var lineY = headerY2 + lineSpacing
                        while (lineY < h + 250.dp.toPx()) {
                            drawLine(
                                color = ruledLineColor,
                                start = Offset(0f, lineY),
                                end = Offset(w, lineY),
                                strokeWidth = 1.35.dp.toPx()
                            )
                            lineY += lineSpacing
                        }
                    }
            ) {
                // Header Area: Note Title and Date / Page box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .padding(start = 54.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title Input Field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.KANNADA) "ಶೀರ್ಷಿಕೆ (Title)" else "Note Title",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        BasicTextField(
                            value = page.title,
                            onValueChange = onTitleChange,
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notebook_title_input"),
                            decorationBox = { innerTextField ->
                                if (page.title.isEmpty()) {
                                    Text(
                                        text = if (language == AppLanguage.KANNADA) "ಉದಾ: ದಿನಸಿ, ಬಾಕಿ ಲೆಕ್ಕ..." else "e.g. Grocery, Ledger...",
                                        color = textColor.copy(alpha = 0.4f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Authentic Notebook Date & Page Box (Indian Notebook style corner box - enlarged date)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, marginLineColor.copy(alpha = 0.65f)),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (language == AppLanguage.KANNADA) "ದಿನಾಂಕ: $formattedDate" else "Date: $formattedDate",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (language == AppLanguage.KANNADA) "ಪುಟ: ${pageIndex + 1}" else "Page: ${pageIndex + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Space for double header line
                Spacer(modifier = Modifier.height(6.dp))

                // Editable Ruled Body Text Field
                BasicTextField(
                    value = page.content,
                    onValueChange = onContentChange,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 36.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 54.dp, end = 16.dp, top = 6.dp, bottom = 140.dp)
                        .testTag("notebook_content_input"),
                    decorationBox = { innerTextField ->
                        if (page.content.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.KANNADA)
                                    "ಇಲ್ಲಿ ಬರೆಯಿರಿ...\nಉದಾಹರಣೆಗೆ:\n1. ಅಂಗಡಿ ಬಾಕಿ: 2,500\n2. ಮನೆ ಸಾಮಾನುಗಳು: 1,200\n3. ಒಟ್ಟು ಮೊತ್ತ: 3,700"
                                else
                                    "Write your notes here...\nFor example:\n1. Shop balance: 2,500\n2. Household items: 1,200\n3. Total amount: 3,700",
                                color = textColor.copy(alpha = 0.38f),
                                fontSize = 16.sp,
                                lineHeight = 36.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

/**
 * Transparent floating keyboard overlay with Letters (ಕನ್ನಡ / English A-Z), Numbers, and Symbols.
 */
@Composable
fun TransparentNotebookKeypad(
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    language: AppLanguage,
    onCharInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onNewLine: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onHide: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Mode tab: 0 = "ಕನ್ನಡ", 1 = "ABC", 2 = "123/ಚಿಹ್ನೆಗಳು"
    var keypadMode by remember { mutableStateOf(if (language == AppLanguage.KANNADA) 0 else 1) }

    // Translucent white card backing for white notebook theme
    val bgCardColor = Color(0xFFFFFFFF).copy(alpha = alpha.coerceIn(0.25f, 0.98f))
    val keyBgColor = Color(0xFFF1F5F9).copy(alpha = (alpha * 0.95f).coerceIn(0.30f, 0.98f))
    val actionBgColor = MaterialTheme.colorScheme.primary.copy(alpha = (alpha * 0.95f).coerceIn(0.40f, 0.98f))
    val contentColor = Color(0xFF0F172A)
    val actionContentColor = MaterialTheme.colorScheme.onPrimary
    val borderColor = Color(0xFFCBD5E1).copy(alpha = alpha)

    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transparent_notebook_keypad")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Top Toolbar: Tabs (ಕನ್ನಡ / ABC / 123) + Transparency Slider + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mode Selectors: [ಕನ್ನಡ] [ABC] [123]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ಕನ್ನಡ", "ABC", "123").forEachIndexed { idx, label ->
                        val isSelected = keypadMode == idx
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else keyBgColor,
                            border = BorderStroke(0.8.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { keypadMode = idx }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else contentColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Transparency Label & Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Slider(
                        value = alpha,
                        onValueChange = onAlphaChange,
                        valueRange = 0.25f..0.95f,
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${(alpha * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.75f),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Close / Hide Keypad button
                Surface(
                    shape = CircleShape,
                    color = keyBgColor,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onHide() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardHide,
                            contentDescription = "Hide Keypad",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (keypadMode) {
                0 -> {
                    // --- KANNADA LETTERS LAYOUT ---
                    // Row 1: Swaragalu (ಅ, ಆ, ಇ, ಈ, ಉ, ಊ, ಎ, ಏ, ಒ, ಓ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಎ", "ಏ", "ಒ", "ಓ").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 15) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2: Vyanjanagalu Group 1 (ಕ, ಖ, ಗ, ಘ, ಚ, ಛ, ಜ, ಝ, ಟ, ಠ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಚ", "ಛ", "ಜ", "ಝ", "ಟ", "ಠ").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 15) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 3: Vyanjanagalu Group 2 (ಡ, ಢ, ಣ, ತ, ಥ, ದ, ಧ, ನ, ಪ, ಫ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ", "ಪ", "ಫ").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 15) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 4: Vyanjanagalu Group 3 & Gunithakshara signs (ಬ, ಭ, ಮ, ಯ, ರ, ಲ, ವ, ಶ, ಸ, ಹ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಸ", "ಹ").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 15) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 5: Matras (ಾ, ಿ, ೀ, ು, ೂ, ೆ, ೇ, ೊ, ೋ, ್, ಂ, ಃ) & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೆ", "ೇ", "್", "ಂ", "ಃ").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 15) { onCharInput(char) }
                        }
                    }
                }
                1 -> {
                    // --- ENGLISH A-Z LETTERS LAYOUT ---
                    // Row 1: Q W E R T Y U I O P
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 16, isBold = true) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2: A S D F G H J K L
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 16, isBold = true) { onCharInput(char) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 3: Z X C V B N M @ .
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("Z", "X", "C", "V", "B", "N", "M", "@", ".").forEach { char ->
                            TransKeyBtn(char, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 16, isBold = true) { onCharInput(char) }
                        }
                    }
                }
                2 -> {
                    // --- NUMBERS & FINANCIAL SYMBOLS (123) ---
                    // Row 1: ₹, +, -, ×, ÷, =, %, :
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("₹", "+", "-", "×", "÷", "=", "%", ":").forEach { sym ->
                            TransKeyBtn(sym, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 17, isBold = true) { onCharInput(sym) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2: 7, 8, 9, (, ), ,
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("7", "8", "9", "(", ")", ",").forEach { sym ->
                            TransKeyBtn(sym, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 17, isBold = true) { onCharInput(sym) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 3: 4, 5, 6, 0, 00, .
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("4", "5", "6", "0", "00", ".").forEach { sym ->
                            TransKeyBtn(sym, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 17, isBold = true) { onCharInput(sym) }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 4: 1, 2, 3, /, #
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("1", "2", "3", "/", "#").forEach { sym ->
                            TransKeyBtn(sym, keyBgColor, contentColor, borderColor, Modifier.weight(1f), fontSize = 17, isBold = true) { onCharInput(sym) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Unified Action Bar: Space, Backspace, Enter, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Clear page button
                TransKeyBtn(
                    text = if (language == AppLanguage.KANNADA) "ಕ್ಲಿಯರ್" else "Clear",
                    bgColor = keyBgColor,
                    contentColor = MaterialTheme.colorScheme.error,
                    borderColor = borderColor,
                    fontSize = 11,
                    modifier = Modifier.weight(1f),
                    onClick = onClear
                )

                // Space Bar
                TransKeyBtn(
                    text = if (language == AppLanguage.KANNADA) "ಸ್ಪೇಸ್ (Space)" else "Space",
                    bgColor = keyBgColor,
                    contentColor = contentColor,
                    borderColor = borderColor,
                    fontSize = 12,
                    modifier = Modifier.weight(2.5f),
                    onClick = onSpace
                )

                // Backspace ⌫
                TransKeyBtn(
                    text = "⌫",
                    bgColor = if (isDark) Color(0xFFEF4444).copy(alpha = alpha * 0.45f) else Color(0xFFFCA5A5).copy(alpha = alpha * 0.65f),
                    contentColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                    borderColor = borderColor,
                    fontSize = 17,
                    isBold = true,
                    modifier = Modifier.weight(1f),
                    onClick = onBackspace
                )

                // Enter / New Line
                TransKeyBtn(
                    text = "↵ Enter",
                    bgColor = actionBgColor,
                    contentColor = actionContentColor,
                    borderColor = borderColor,
                    fontSize = 12,
                    isBold = true,
                    modifier = Modifier.weight(1.3f),
                    onClick = onNewLine
                )
            }
        }
    }
}

@Composable
private fun TransKeyBtn(
    text: String,
    bgColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    isBold: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                color = contentColor,
                fontSize = fontSize.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
