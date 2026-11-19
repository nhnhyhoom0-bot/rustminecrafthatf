package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "rustcraft_settings")

data class LauncherSettings(
    val defaultRamMb: Int = 2560,
    val javaRuntime: String = "Java 21 (Temurin ARM64)",
    val renderer: String = "Holy GL4ES",
    val resolutionScale: Float = 0.85f,
    val autoScanOnStartup: Boolean = true,
    val defaultSkinServer: String = "https://authserver.ely.by/api/authlib-injector",
    val savedScannerTreeUris: Set<String> = emptySet(),
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_RAM_MB = intPreferencesKey("default_ram_mb")
        val JAVA_RUNTIME = stringPreferencesKey("java_runtime")
        val RENDERER = stringPreferencesKey("renderer")
        val RESOLUTION_SCALE = floatPreferencesKey("resolution_scale")
        val AUTO_SCAN_ON_STARTUP = booleanPreferencesKey("auto_scan_on_startup")
        val DEFAULT_SKIN_SERVER = stringPreferencesKey("default_skin_server")
        val SAVED_SCANNER_TREES = stringSetPreferencesKey("saved_scanner_tree_uris")
    }

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data.map { prefs ->
        LauncherSettings(
            defaultRamMb = prefs[Keys.DEFAULT_RAM_MB] ?: 2560,
            javaRuntime = prefs[Keys.JAVA_RUNTIME] ?: "Java 21 (Temurin ARM64)",
            renderer = prefs[Keys.RENDERER] ?: "Holy GL4ES",
            resolutionScale = prefs[Keys.RESOLUTION_SCALE] ?: 0.85f,
            autoScanOnStartup = prefs[Keys.AUTO_SCAN_ON_STARTUP] ?: true,
            defaultSkinServer = prefs[Keys.DEFAULT_SKIN_SERVER]
                ?: "https://authserver.ely.by/api/authlib-injector",
            savedScannerTreeUris = prefs[Keys.SAVED_SCANNER_TREES] ?: emptySet(),
        )
    }

    suspend fun updateRam(ramMb: Int) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_RAM_MB] = ramMb }
    }

    suspend fun updateJavaRuntime(runtime: String) {
        context.settingsDataStore.edit { it[Keys.JAVA_RUNTIME] = runtime }
    }

    suspend fun updateRenderer(renderer: String) {
        context.settingsDataStore.edit { it[Keys.RENDERER] = renderer }
    }

    suspend fun updateResolutionScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.RESOLUTION_SCALE] = scale }
    }

    suspend fun updateAutoScanOnStartup(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SCAN_ON_STARTUP] = enabled }
    }

    suspend fun updateDefaultSkinServer(url: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_SKIN_SERVER] = url }
    }

    suspend fun addScannerTreeUri(uriString: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_SCANNER_TREES] ?: emptySet()
            prefs[Keys.SAVED_SCANNER_TREES] = current + uriString
        }
    }

    suspend fun clearScannerTreeUris() {
        context.settingsDataStore.edit { it.remove(Keys.SAVED_SCANNER_TREES) }
    }
}
