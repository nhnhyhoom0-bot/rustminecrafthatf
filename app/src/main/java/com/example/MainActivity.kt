package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.LauncherViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModsScreen
import com.example.ui.screens.PlayOverlayScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkinsAccountsScreen
import com.example.ui.screens.TouchEditorScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RustCraftTheme

object Routes {
    const val ROUTE_HOME = "home_screen"
    const val ROUTE_MODS = "mods_screen"
    const val ROUTE_SKINS = "skins_accounts_screen"
    const val ROUTE_TOUCH = "touch_editor_screen"
    const val ROUTE_SETTINGS = "settings_screen"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RustCraftTheme {
                // Force RTL layout direction for natural Arabic UI alignment
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RustCraftApp()
                }
            }
        }
    }
}

@Composable
fun RustCraftApp(
    viewModel: LauncherViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(Routes.ROUTE_HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            NavDestinationItem(Routes.ROUTE_MODS, R.string.nav_mods, Icons.Rounded.Extension, Icons.Outlined.Extension, "nav_mods"),
            NavDestinationItem(Routes.ROUTE_SKINS, R.string.nav_skins, Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, "nav_skins"),
            NavDestinationItem(Routes.ROUTE_TOUCH, R.string.nav_touch, Icons.Rounded.Gamepad, Icons.Outlined.Gamepad, "nav_touch"),
            NavDestinationItem(Routes.ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings"),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail && !uiState.isPlayOverlayActive) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail && !uiState.isPlayOverlayActive) {
                        NavigationRail(containerColor = ObsidianSurface) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val label = stringResource(item.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("${item.testTag}_rail"),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.ROUTE_HOME,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(Routes.ROUTE_HOME) {
                            HomeScreen(
                                uiState = uiState,
                                onPlayClick = viewModel::launchSelectedInstance,
                                onScanReadyVersions = { viewModel.scanForReadyVersions(silentIfEmpty = false) },
                                onImportInstanceFolder = viewModel::importExternalInstance,
                                onSelectInstance = viewModel::selectInstance,
                                onDeleteInstance = viewModel::deleteInstance,
                                onOpenExternalLauncher = viewModel::openExternalLauncher,
                                onNavigateToMods = { navigateTo(Routes.ROUTE_MODS) },
                                onNavigateToSkins = { navigateTo(Routes.ROUTE_SKINS) },
                            )
                        }
                        composable(Routes.ROUTE_MODS) {
                            ModsScreen(
                                uiState = uiState,
                                onSelectTab = viewModel::setModStoreTab,
                                onQueryChange = viewModel::updateModSearchQuery,
                                onSearchSubmit = viewModel::searchMods,
                                onFilterChange = viewModel::updateModFilters,
                                onOpenModVersions = viewModel::openModVersionPicker,
                                onCloseModVersions = viewModel::closeModVersionPicker,
                                onInstallModVersion = viewModel::installModVersion,
                                onImportLocalMod = viewModel::importLocalMod,
                                onToggleMod = viewModel::toggleMod,
                                onDeleteMod = viewModel::deleteMod,
                            )
                        }
                        composable(Routes.ROUTE_SKINS) {
                            SkinsAccountsScreen(
                                uiState = uiState,
                                onImportSkinUri = viewModel::importSkinUri,
                                onFetchSkinByUsername = viewModel::fetchSkinFromUsername,
                                onUpdateSkinModel = viewModel::updateSkinModel,
                                onUpdateCape = viewModel::updateCapeStyle,
                                onUpdateSkinServer = viewModel::updateSkinServerUrl,
                                onSyncSkinToServers = viewModel::syncSkinToMultiplayerServers,
                                onAddOfflineAccount = viewModel::addOfflineAccount,
                                onActivateAccount = viewModel::activateAccount,
                                onDeleteAccount = viewModel::deleteAccount,
                                onStartMicrosoftLogin = viewModel::startMicrosoftLogin,
                                onResetMicrosoftLogin = viewModel::resetMicrosoftLoginState,
                                onShowMessage = viewModel::showMessage,
                            )
                        }
                        composable(Routes.ROUTE_TOUCH) {
                            TouchEditorScreen(
                                uiState = uiState,
                                onActivateLayout = viewModel::activateTouchLayout,
                                onCreateLayout = viewModel::createTouchLayout,
                                onSaveButtons = viewModel::saveTouchButtons,
                                onUpdateLayoutPrefs = viewModel::updateTouchLayoutPreferences,
                            )
                        }
                        composable(Routes.ROUTE_SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                onScanReadyVersions = { viewModel.scanForReadyVersions(silentIfEmpty = false) },
                                onToggleAutoScan = viewModel::updateAutoScanOnStartup,
                                onClearScannerFolders = viewModel::clearSavedScannerFolders,
                                onUpdateRam = viewModel::updateRam,
                                onUpdateJavaRuntime = viewModel::updateJavaRuntime,
                                onUpdateRenderer = viewModel::updateRenderer,
                                onUpdateResolutionScale = viewModel::updateResolutionScale,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isPlayOverlayActive) {
            PlayOverlayScreen(
                uiState = uiState,
                onClose = viewModel::closePlayOverlay,
            )
        }
    }
}
