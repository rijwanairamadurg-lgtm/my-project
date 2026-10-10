package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppLanguage
import com.example.data.repository.AppTheme
import com.example.ui.MainViewModel
import com.example.util.Localization
import com.example.util.Localization.StringKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsState()
    val language = settings.language

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Localization.getString(StringKey.SETTINGS, language),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Localization.getString(StringKey.THEME, language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ThemeRadioOption(
                        label = Localization.getString(StringKey.THEME_DARK, language),
                        selected = settings.theme == AppTheme.DARK,
                        onClick = { viewModel.updateTheme(AppTheme.DARK) }
                    )
                    ThemeRadioOption(
                        label = Localization.getString(StringKey.THEME_LIGHT, language),
                        selected = settings.theme == AppTheme.LIGHT,
                        onClick = { viewModel.updateTheme(AppTheme.LIGHT) }
                    )
                    ThemeRadioOption(
                        label = Localization.getString(StringKey.THEME_SYSTEM, language),
                        selected = settings.theme == AppTheme.SYSTEM,
                        onClick = { viewModel.updateTheme(AppTheme.SYSTEM) }
                    )
                }
            }

            // Language Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Localization.getString(StringKey.LANGUAGE, language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ThemeRadioOption(
                        label = Localization.getString(StringKey.LANGUAGE_ENGLISH, language),
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { viewModel.updateLanguage(AppLanguage.ENGLISH) }
                    )
                    ThemeRadioOption(
                        label = Localization.getString(StringKey.LANGUAGE_KANNADA, language),
                        selected = settings.language == AppLanguage.KANNADA,
                        onClick = { viewModel.updateLanguage(AppLanguage.KANNADA) }
                    )
                }
            }

            // Calculation & Behavior Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.KANNADA) "ಕ್ಯಾಲ್ಕುಲೇಟರ್ ನಡವಳಿಕೆ" else "Calculator Behavior",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Default BODMAS
                    SettingSwitchRow(
                        title = if (language == AppLanguage.KANNADA) "ಡೀಫಾಲ್ಟ್ ಬೋಡ್ಮಾಸ್ ಮೋಡ್" else "Default BODMAS Mode",
                        subtitle = if (language == AppLanguage.KANNADA) "ಹೊಸ ಪುಟಗಳಲ್ಲಿ ಪ್ರಮಾಣಿತ ಗಣಿತದ ಕ್ರಮವನ್ನು ಬಳಸಿ" else "Use standard mathematical order of operations on new pages",
                        checked = settings.defaultBodmas,
                        onCheckedChange = { viewModel.updateDefaultBodmas(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Haptic Feedback
                    SettingSwitchRow(
                        title = Localization.getString(StringKey.HAPTIC_FEEDBACK, language),
                        subtitle = if (language == AppLanguage.KANNADA) "ಬಟನ್ ಒತ್ತಿದಾಗ ಸೌಮ್ಯ ಕಂಪನ" else "Vibrate slightly when buttons are pressed",
                        checked = settings.hapticFeedback,
                        onCheckedChange = { viewModel.updateHapticFeedback(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Auto Save History
                    SettingSwitchRow(
                        title = Localization.getString(StringKey.AUTO_SAVE_HISTORY, language),
                        subtitle = if (language == AppLanguage.KANNADA) "ಲೆಕ್ಕಾಚಾರಗಳನ್ನು ಸ್ವಯಂಚಾಲಿತವಾಗಿ ಉಳಿಸಿ" else "Automatically save calculations to database",
                        checked = settings.autoSaveHistory,
                        onCheckedChange = { viewModel.updateAutoSaveHistory(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
