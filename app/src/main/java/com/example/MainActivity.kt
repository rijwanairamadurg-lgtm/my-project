package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.AppDrawerContent
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.age.AgeCalculatorScreen
import com.example.ui.screens.amountwords.AmountToWordsScreen
import com.example.ui.screens.calculator.CalculatorScreen
import com.example.ui.screens.cashcounter.CashNoteCounterScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.notebook.NotebookScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.unitconverter.UnitConverterScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.userSettings.collectAsState()

            MyApplicationTheme(appTheme = settings.theme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Handle back button: if drawer is open, close it; if not on Calculator, go to Calculator
    BackHandler(enabled = drawerState.isOpen || currentScreen != ScreenDestination.CALCULATOR) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen != ScreenDestination.CALCULATOR) {
            viewModel.navigateTo(ScreenDestination.CALCULATOR)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentScreen = currentScreen,
                language = settings.language,
                onNavigate = { dest -> viewModel.navigateTo(dest) },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        val openDrawer: () -> Unit = {
            coroutineScope.launch { drawerState.open() }
        }

        when (currentScreen) {
            ScreenDestination.CALCULATOR -> CalculatorScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.CASH_COUNTER -> CashNoteCounterScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.AMOUNT_TO_WORDS -> AmountToWordsScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.AGE_CALCULATOR -> AgeCalculatorScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.UNIT_CONVERTER -> UnitConverterScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.NOTEBOOK -> NotebookScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.HISTORY -> HistoryScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
            ScreenDestination.ABOUT -> AboutScreen(viewModel = viewModel, onOpenDrawer = openDrawer)
        }
    }
}
