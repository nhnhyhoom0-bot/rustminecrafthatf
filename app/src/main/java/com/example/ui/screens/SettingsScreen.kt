package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.RustCoreBridge
import com.example.ui.LauncherUiState
import com.example.ui.components.RustCraftCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RustOrange
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    uiState: LauncherUiState,
    onScanReadyVersions: () -> Unit,
    onToggleAutoScan: (Boolean) -> Unit,
    onClearScannerFolders: () -> Unit,
    onUpdateRam: (Int) -> Unit,
    onUpdateJavaRuntime: (String) -> Unit,
    onUpdateRenderer: (String) -> Unit,
    onUpdateResolutionScale: (Float) -> Unit,
) {
    val settings = uiState.settings
    val activeAcc = uiState.activeAccount
    val selectedInst = uiState.selectedInstance

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // External Ready-Version Scanner Settings
        item {
            RustCraftCard(
                modifier = Modifier.widthIn(max = 680.dp),
                borderColor = EmeraldPrimary.copy(alpha = 0.45f),
            ) {
                SectionHeader(
                    title = "إعدادات البحث عن النسخ الجاهزة",
                    icon = Icons.Rounded.ManageSearch,
                    accent = EmeraldPrimary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("بحث تلقائي عند تشغيل اللانشر", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "يفحص مجلدات لانشرات Zalith وMojo وPojav تلقائياً عند الفتح",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = settings.autoScanOnStartup,
                        onCheckedChange = onToggleAutoScan,
                    )
                }

                Text(
                    text = "المجلدات المرتبطة المحفوظة: ${settings.savedScannerTreeUris.size} • النسخ المكتشفة: ${uiState.instances.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiamondCyan,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onScanReadyVersions,
                        enabled = !uiState.isScanningVersions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_rescan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    ) {
                        if (uiState.isScanningVersions) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة الفحص الآن", fontWeight = FontWeight.Bold)
                    }

                    if (settings.savedScannerTreeUris.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearScannerFolders,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح المسارات")
                        }
                    }
                }
            }
        }

        // Performance & Graphics Settings
        item {
            RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                SectionHeader(
                    title = "إعدادات الأداء والرسوميات",
                    icon = Icons.Rounded.Tune,
                    accent = DiamondCyan,
                )

                Text(
                    text = "الذاكرة الافتراضية (RAM): ${settings.defaultRamMb} MB",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.defaultRamMb.toFloat(),
                    onValueChange = { onUpdateRam((it.toInt() / 256) * 256) },
                    valueRange = 1024f..8192f,
                )

                Text(
                    text = "دقة العرض (Resolution Scale): ${(settings.resolutionScale * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.resolutionScale,
                    onValueChange = onUpdateResolutionScale,
                    valueRange = 0.5f..1.0f,
                )

                Text("بيئة Java Runtime:", style = MaterialTheme.typography.labelLarge)
                val runtimes = listOf(
                    "Java 21 (Temurin ARM64)",
                    "Java 17 (LTS ARM64)",
                    "Java 8 (Legacy 1.12)",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(runtimes) { rt ->
                        FilterChip(
                            selected = settings.javaRuntime == rt,
                            onClick = { onUpdateJavaRuntime(rt) },
                            label = { Text(rt) },
                        )
                    }
                }

                Text("معالج الرسوميات (Renderer):", style = MaterialTheme.typography.labelLarge)
                val renderers = listOf("Holy GL4ES", "Zink Vulkan", "Angle", "VirGL")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(renderers) { rnd ->
                        FilterChip(
                            selected = settings.renderer == rnd,
                            onClick = { onUpdateRenderer(rnd) },
                            label = { Text(rnd) },
                        )
                    }
                }
            }
        }

        // Rust Core Engine & JNI Diagnostics
        item {
            val previewArgs = RustCoreBridge.buildJvmLaunchArgs(
                ramMb = selectedInst?.ramMb ?: settings.defaultRamMb,
                username = activeAcc?.username ?: "RustBuilder",
                uuid = activeAcc?.uuid ?: RustCoreBridge.generateOfflineUuid("RustBuilder"),
                mcVersion = selectedInst?.mcVersion ?: "External-Ready",
                loader = selectedInst?.loaderType ?: "EXTERNAL",
                skinServerUrl = activeAcc?.skinServerUrl,
                renderer = settings.renderer,
            )

            RustCraftCard(
                modifier = Modifier.widthIn(max = 680.dp),
                borderColor = RustOrange.copy(alpha = 0.55f),
            ) {
                SectionHeader(
                    title = "نواة Rust + جسر JNI",
                    icon = Icons.Rounded.Code,
                    accent = RustOrange,
                )
                Text(
                    text = RustCoreBridge.getEngineBanner(),
                    style = MaterialTheme.typography.titleMedium,
                    color = RustOrange,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(text = RustCoreBridge.engineModeLabel, color = DiamondCyan)
                }
                Text(
                    text = "المعماريات المدعومة: ${RustCoreBridge.supportedAbis}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = "الوحدات في rust_core/: auth.rs • manifest.rs • mods.rs • skins.rs • touch.rs • jni_bridge.rs",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = previewArgs.joinToString(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                        )
                    }
                }
            }
        }
    }
}
