package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ModSearchItem
import com.example.data.model.ModVersionOption
import com.example.ui.LauncherUiState
import com.example.ui.ModStoreTab
import com.example.ui.components.RustCraftCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.loaderColor
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RustOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen(
    uiState: LauncherUiState,
    onSelectTab: (ModStoreTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onFilterChange: (loader: String?, version: String?) -> Unit,
    onOpenModVersions: (ModSearchItem) -> Unit,
    onCloseModVersions: () -> Unit,
    onInstallModVersion: (ModVersionOption) -> Unit,
    onImportLocalMod: (Uri) -> Unit,
    onToggleMod: (Long, Boolean) -> Unit,
    onDeleteMod: (Long) -> Unit,
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportLocalMod(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Store Switcher (Modrinth / CurseForge / Installed)
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ModStoreTab.entries.forEachIndexed { idx, tab ->
                        val label = when (tab) {
                            ModStoreTab.MODRINTH -> "Modrinth"
                            ModStoreTab.CURSEFORGE -> "CurseForge"
                            ModStoreTab.INSTALLED -> "المثبتة (${uiState.installedMods.size})"
                        }
                        SegmentedButton(
                            selected = uiState.modStoreTab == tab,
                            onClick = { onSelectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = ModStoreTab.entries.size,
                            ),
                            modifier = Modifier.testTag("mod_tab_${tab.name.lowercase()}"),
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                if (uiState.modStoreTab != ModStoreTab.INSTALLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = uiState.modSearchQuery,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text("ابحث عن مود (مثل Sodium, Iris, JEI)...")
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = "بحث")
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mod_search_input"),
                        )
                        Button(
                            onClick = onSearchSubmit,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("mod_search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.modStoreTab == ModStoreTab.MODRINTH) {
                                    EmeraldPrimary
                                } else {
                                    RustOrange
                                },
                            ),
                        ) {
                            Text("بحث")
                        }
                    }

                    // Loader Filter Chips
                    val loaders = listOf("ALL" to "كل المحملات", "FABRIC" to "Fabric", "FORGE" to "Forge", "NEOFORGE" to "NeoForge", "QUILT" to "Quilt")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(loaders) { (key, label) ->
                            FilterChip(
                                selected = uiState.modLoaderFilter == key,
                                onClick = { onFilterChange(key, null) },
                                label = { Text(label) },
                            )
                        }
                    }

                    // Quick Topic Chips
                    val quickTopics = listOf("sodium" to "أداء FPS", "iris" to "شيدرز", "jei" to "وصفات", "xaero" to "خريطة", "create" to "Create")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickTopics) { (q, label) ->
                            FilterChip(
                                selected = uiState.modSearchQuery.equals(q, ignoreCase = true),
                                onClick = {
                                    onQueryChange(q)
                                    onSearchSubmit()
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_local_mod_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد مود أو حزمة (.jar / .mrpack / .zip) من الجهاز")
                    }
                }
            }
        }

        // Content List
        if (uiState.modStoreTab == ModStoreTab.INSTALLED) {
            if (uiState.installedMods.isEmpty()) {
                item {
                    RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp),
                            )
                            Text("لا توجد مودات مركبة في هذه النسخة بعد")
                            Text(
                                text = "تصفح Modrinth أو CurseForge بالأعلى وقم بتركيب المودات بضغطة واحدة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            } else {
                items(uiState.installedMods, key = { it.id }) { mod ->
                    RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ModIconBox(mod.iconUrl)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mod.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${mod.versionNumber} • ${mod.source}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = mod.isEnabled,
                                onCheckedChange = { onToggleMod(mod.id, it) },
                            )
                            IconButton(onClick = { onDeleteMod(mod.id) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف المود",
                                    tint = TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (uiState.isSearchingMods) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }
            } else if (uiState.modSearchError != null) {
                item {
                    RustCraftCard(
                        modifier = Modifier.widthIn(max = 680.dp),
                        borderColor = RustOrange.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = uiState.modSearchError,
                            style = MaterialTheme.typography.bodyLarge,
                            color = RustOrange,
                        )
                        if (uiState.modStoreTab == ModStoreTab.CURSEFORGE) {
                            Text(
                                text = "ملاحظة: متجر Modrinth يعمل فوراً بدون أي مفتاح، أما البحث الحي في CurseForge فيتطلب إضافة CURSEFORGE_API_KEY في لوحة Secrets في AI Studio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            } else {
                items(uiState.modSearchResults, key = { "${it.source}_${it.projectId}" }) { mod ->
                    ModSearchResultCard(
                        mod = mod,
                        onInstallClick = { onOpenModVersions(mod) },
                    )
                }
            }
        }
    }

    // Version Picker Bottom Sheet
    val modForInstall = uiState.selectedModForInstall
    if (modForInstall != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseModVersions,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "تركيب ${modForInstall.title}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "اختر الإصدار المناسب للنسخة (${uiState.selectedInstance?.mcVersion ?: "1.21.4"}):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                if (uiState.isLoadingModVersions) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.modVersionOptions) { ver ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianSurfaceElevated,
                                border = BorderStroke(1.dp, ObsidianBorder),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ver.versionNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "${ver.loaders.joinToString("/").ifEmpty { "Universal" }} • ${ver.gameVersions.take(3).joinToString(", ")}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                        )
                                    }
                                    Button(
                                        onClick = { onInstallModVersion(ver) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تركيب")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ModSearchResultCard(
    mod: ModSearchItem,
    onInstallClick: () -> Unit,
) {
    RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModIconBox(mod.iconUrl)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = mod.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StatusBadge(
                        text = mod.source,
                        color = if (mod.source == "MODRINTH") EmeraldPrimary else RustOrange,
                    )
                }
                Text(
                    text = "بواسطة ${mod.author} • ${formatDownloads(mod.downloads)} تحميل",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiamondCyan,
                )
                Text(
                    text = mod.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mod.loaders.take(3).forEach { ldr ->
                    StatusBadge(text = ldr, color = loaderColor(ldr))
                }
            }
            Button(
                onClick = onInstallClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اختيار وتركيب", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModIconBox(iconUrl: String?) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = "أيقونة المود",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.Extension,
                contentDescription = "مود",
                tint = EmeraldPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private fun formatDownloads(count: Long): String = when {
    count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
    count >= 1_000 -> "${"%.1f".format(count / 1_000.0)}K"
    else -> count.toString()
}
