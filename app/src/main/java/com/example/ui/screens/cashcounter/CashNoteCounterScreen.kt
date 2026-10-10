package com.example.ui.screens.cashcounter

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalculatorEngine
import com.example.ui.MainViewModel
import com.example.util.IndianNumberToWords
import com.example.util.Localization
import com.example.util.Localization.StringKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashNoteCounterScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val noteCounts by viewModel.noteCounts.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val language = settings.language

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Calculate totals
    var totalNotes = 0
    var grandTotalAmount = 0L

    viewModel.noteDenominations.forEach { denom ->
        val count = noteCounts[denom] ?: 0
        totalNotes += count
        grandTotalAmount += (denom.toLong() * count)
    }

    val amountInWordsEn = IndianNumberToWords.convertToEnglish(grandTotalAmount.toString())
    val amountInWordsKn = IndianNumberToWords.convertToKannada(grandTotalAmount.toString())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Localization.getString(StringKey.CASH_NOTE_COUNTER, language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("reset_cash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = Localization.getString(StringKey.RESET_ALL, language)
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
            // Scrollable list of denominations
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.noteDenominations) { denom ->
                    val count = noteCounts[denom] ?: 0
                    val subtotal = denom.toLong() * count

                    DenominationRowCard(
                        denomination = denom,
                        count = count,
                        subtotal = subtotal,
                        onCountChange = { newCount -> viewModel.updateNoteCount(denom, newCount) },
                        onIncrement = { delta -> viewModel.incrementNoteCount(denom, delta) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sticky Bottom Summary Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Total Notes & Grand Total Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Localization.getString(StringKey.TOTAL_NOTES, language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalNotes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Localization.getString(StringKey.GRAND_TOTAL, language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹ ${CalculatorEngine.formatIndianNumber(grandTotalAmount.toString())}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Live Amount in Words
                    if (grandTotalAmount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == com.example.data.repository.AppLanguage.KANNADA) amountInWordsKn else amountInWordsEn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons (Copy Slip & Share Slip)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val breakdownText = buildBreakdownText(
                            noteCounts = noteCounts,
                            denominations = viewModel.noteDenominations,
                            totalNotes = totalNotes,
                            grandTotal = grandTotalAmount,
                            wordsEn = amountInWordsEn
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(breakdownText))
                                    Toast.makeText(
                                        context,
                                        Localization.getString(StringKey.COPIED_TO_CLIPBOARD, language),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("copy_cash_summary")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString(StringKey.COPY_SUMMARY, language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, breakdownText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Cash Breakdown")
                                    context.startActivity(shareIntent)
                                }
                                .testTag("share_cash_summary")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString(StringKey.SHARE_SUMMARY, language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(text = Localization.getString(StringKey.RESET_ALL, language))
            },
            text = {
                Text(text = "Are you sure you want to reset all note counts to 0?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetNoteCounts()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text(
                        text = Localization.getString(StringKey.CONFIRM, language),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false }
                ) {
                    Text(text = Localization.getString(StringKey.CANCEL, language))
                }
            }
        )
    }
}

@Composable
fun DenominationRowCard(
    denomination: Int,
    count: Int,
    subtotal: Long,
    onCountChange: (Int) -> Unit,
    onIncrement: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Denomination distinctive color
    val badgeColor = when (denomination) {
        2000 -> Color(0xFFC026D3) // Magenta
        500 -> Color(0xFF65A30D)  // Stone Green
        200 -> Color(0xFFEA580C)  // Bright Orange
        100 -> Color(0xFF0284C7)  // Lavender/Cyan Blue
        50 -> Color(0xFF0D9488)   // Teal / Cyan
        20 -> Color(0xFFCA8A04)   // Yellow / Ochre
        10 -> Color(0xFF78716C)   // Chocolate / Warm Slate
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Denomination Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.width(72.dp)
                ) {
                    Text(
                        text = "₹ $denomination",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                // Stepper Buttons & Count Input Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Decrement Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onIncrement(-1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minus",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Count text field
                    OutlinedTextField(
                        value = if (count == 0) "" else count.toString(),
                        onValueChange = {
                            val clean = it.filter { c -> c.isDigit() }
                            val newCount = clean.toIntOrNull() ?: 0
                            onCountChange(newCount)
                        },
                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .width(80.dp)
                            .height(48.dp)
                            .testTag("count_input_$denomination"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // Increment Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onIncrement(1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Subtotal
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${CalculatorEngine.formatIndianNumber(subtotal.toString())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (subtotal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Quick add chips row (+5, +10, +50, +100)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 50, 100).forEach { addVal ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onIncrement(addVal) }
                    ) {
                        Text(
                            text = "+$addVal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildBreakdownText(
    noteCounts: Map<Int, Int>,
    denominations: List<Int>,
    totalNotes: Int,
    grandTotal: Long,
    wordsEn: String
): String {
    val sb = java.lang.StringBuilder()
    sb.append("💵 CASH NOTE BREAKDOWN\n")
    sb.append("========================\n")
    denominations.forEach { denom ->
        val count = noteCounts[denom] ?: 0
        if (count > 0) {
            val subtotal = denom.toLong() * count
            sb.append("₹$denom × $count = ₹${CalculatorEngine.formatIndianNumber(subtotal.toString())}\n")
        }
    }
    sb.append("========================\n")
    sb.append("Total Notes: $totalNotes\n")
    sb.append("Grand Total: ₹${CalculatorEngine.formatIndianNumber(grandTotal.toString())}\n")
    sb.append("In Words: $wordsEn\n")
    return sb.toString()
}
