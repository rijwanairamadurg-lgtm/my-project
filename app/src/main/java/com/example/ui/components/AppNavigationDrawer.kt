package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppLanguage
import com.example.ui.ScreenDestination
import com.example.util.Localization
import com.example.util.Localization.StringKey

data class DrawerMenuItem(
    val destination: ScreenDestination,
    val titleKey: StringKey,
    val icon: ImageVector,
    val subtitleEn: String,
    val subtitleKn: String
)

@Composable
fun AppDrawerContent(
    currentScreen: ScreenDestination,
    language: AppLanguage,
    onNavigate: (ScreenDestination) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val menuItems = listOf(
        DrawerMenuItem(
            destination = ScreenDestination.CALCULATOR,
            titleKey = StringKey.CALCULATOR,
            icon = Icons.Default.Calculate,
            subtitleEn = "Multi-Screen & BODMAS",
            subtitleKn = "ಮಲ್ಟಿ-ಸ್ಕ್ರೀನ್ ಮತ್ತು ಬೋಡ್ಮಾಸ್"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.CASH_COUNTER,
            titleKey = StringKey.CASH_NOTE_COUNTER,
            icon = Icons.Default.Payments,
            subtitleEn = "₹ Denomination Tally",
            subtitleKn = "ಭಾರತೀಯ ನೋಟುಗಳ ಲೆಕ್ಕ"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.AMOUNT_TO_WORDS,
            titleKey = StringKey.AMOUNT_TO_WORDS,
            icon = Icons.Default.Abc,
            subtitleEn = "English & Kannada Words",
            subtitleKn = "ಇಂಗ್ಲಿಷ್ ಮತ್ತು ಕನ್ನಡ ಅಕ್ಷರಗಳು"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.AGE_CALCULATOR,
            titleKey = StringKey.AGE_CALCULATOR,
            icon = Icons.Default.Cake,
            subtitleEn = "Years, Months, Days & Next B'day",
            subtitleKn = "ನಿಖರ ವಯಸ್ಸು ಮತ್ತು ಜನ್ಮದಿನ"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.UNIT_CONVERTER,
            titleKey = StringKey.UNIT_CONVERTER,
            icon = Icons.Default.ChangeCircle,
            subtitleEn = "Length, Weight, Area, Volume...",
            subtitleKn = "ಉದ್ದ, ತೂಕ, ವಿಸ್ತೀರ್ಣ, ಇತ್ಯಾದಿ"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.NOTEBOOK,
            titleKey = StringKey.NOTEBOOK,
            icon = Icons.Default.Book,
            subtitleEn = "Multi-page Notes with Transparent Keypad",
            subtitleKn = "ನೋಟ್‌ಬುಕ್ ಪುಟಗಳು ಮತ್ತು ಪಾರದರ್ಶಕ ಕೀಪ್ಯಾಡ್"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.HISTORY,
            titleKey = StringKey.HISTORY,
            icon = Icons.Default.History,
            subtitleEn = "Saved Calculations",
            subtitleKn = "ಉಳಿಸಿದ ಲೆಕ್ಕಾಚಾರಗಳು"
        )
    )

    val bottomItems = listOf(
        DrawerMenuItem(
            destination = ScreenDestination.SETTINGS,
            titleKey = StringKey.SETTINGS,
            icon = Icons.Default.Settings,
            subtitleEn = "Preferences & Theme",
            subtitleKn = "ಆಯ್ಕೆಗಳು ಮತ್ತು ಥೀಮ್"
        ),
        DrawerMenuItem(
            destination = ScreenDestination.ABOUT,
            titleKey = StringKey.ABOUT,
            icon = Icons.Default.Info,
            subtitleEn = "Version & Info",
            subtitleKn = "ಆ್ಯಪ್ ಮಾಹಿತಿ"
        )
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = Localization.getString(StringKey.APP_TITLE, language),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH) "Smart Multi-Screen Suite" else "ಸ್ಮಾರ್ಟ್ ಮಲ್ಟಿ-ಸ್ಕ್ರೀನ್ ಆ್ಯಪ್",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Items
            menuItems.forEach { item ->
                val selected = currentScreen == item.destination
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = Localization.getString(item.titleKey, language)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = Localization.getString(item.titleKey, language),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (language == AppLanguage.KANNADA) item.subtitleKn else item.subtitleEn,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = selected,
                    onClick = {
                        onNavigate(item.destination)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_${item.destination.name}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Bottom Settings & About Items
            bottomItems.forEach { item ->
                val selected = currentScreen == item.destination
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = Localization.getString(item.titleKey, language)
                        )
                    },
                    label = {
                        Text(
                            text = Localization.getString(item.titleKey, language),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    },
                    selected = selected,
                    onClick = {
                        onNavigate(item.destination)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_item_${item.destination.name}"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
