package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AccountType
import com.example.data.local.ExternalLauncherTarget
import com.example.ui.LauncherUiState
import com.example.ui.components.RustCraftCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkinHeadAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.components.loaderColor
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RustOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    onPlayClick: () -> Unit,
    onScanReadyVersions: () -> Unit,
    onImportInstanceFolder: (Uri) -> Unit,
    onSelectInstance: (Long) -> Unit,
    onDeleteInstance: (Long) -> Unit,
    onOpenExternalLauncher: (String, String) -> Unit,
    onNavigateToMods: () -> Unit,
    onNavigateToSkins: () -> Unit,
) {
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) onImportInstanceFolder(uri)
    }

    val selected = uiState.selectedInstance
    val account = uiState.activeAccount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero Banner Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_launcher),
                    contentDescription = "خلفية لانشر RustCraft",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.25f),
                                    ObsidianBg.copy(alpha = 0.88f),
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusBadge(
                            text = "RUST CORE + JNI",
                            color = RustOrange,
                        )
                        StatusBadge(
                            text = if (selected != null) {
                                "نسخة جاهزة: ${selected.mcVersion}"
                            } else {
                                "في انتظار نسخة خارجية"
                            },
                            color = if (selected != null) EmeraldPrimary else DiamondCyan,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToSkins),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SkinHeadAvatar(
                                skinPath = account?.skinUri.orEmpty(),
                                size = 50.dp,
                            )
                            Column {
                                Text(
                                    text = "RustCraft Launcher",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                )
                                Text(
                                    text = buildString {
                                        append(account?.username ?: "RustBuilder")
                                        append(" • ")
                                        append(
                                            if (account?.accountType == AccountType.MICROSOFT.name) {
                                                "حساب مايكروسوفت أصلي"
                                            } else {
                                                "حساب كراك (Offline)"
                                            }
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ready-Version Scanner Card (Searches ONLY for versions installed by another launcher)
        item {
            RustCraftCard(
                modifier = Modifier.widthIn(max = 680.dp),
                borderColor = EmeraldPrimary.copy(alpha = 0.5f),
            ) {
                SectionHeader(
                    title = "البحث عن نسخة جاهزة مثبتة",
                    icon = Icons.Rounded.ManageSearch,
                    accent = EmeraldPrimary,
                )
                Text(
                    text = "لا يتضمن اللانشر إصدارات مدمجة؛ بل يبحث تلقائياً عن أي نسخة ماين كرافت جاهزة مثبتة مسبقاً عبر لانشر آخر مثل Zalith أو Mojo أو Pojav.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                if (!uiState.lastScanSummary.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                    ) {
                        Text(
                            text = uiState.lastScanSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DiamondCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onScanReadyVersions,
                        enabled = !uiState.isScanningVersions,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scan_ready_versions_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (uiState.isScanningVersions) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بحث تلقائي", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("pick_launcher_folder_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديد مجلد اللانشر", maxLines = 1)
                    }
                }
            }
        }

        // Active Ready Instance Launch Card OR Empty State
        item {
            RustCraftCard(
                modifier = Modifier.widthIn(max = 680.dp),
                borderColor = if (selected != null) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianBorder,
            ) {
                SectionHeader(
                    title = "النسخة الجاهزة المكتشفة",
                    icon = Icons.Rounded.RocketLaunch,
                    accent = if (selected != null) EmeraldPrimary else TextMuted,
                )

                if (selected == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "لا توجد نسخة جاهزة",
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "لم يتم العثور على نسخة جاهزة بعد",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "حمّل نسخة في لانشر Zalith أو Mojo أولاً، ثم اضغط «تحديد مجلد اللانشر» واختر مجلد .minecraft أو versions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(text = "MC ${selected.mcVersion}", color = EmeraldPrimary)
                            StatusBadge(text = selected.loaderType, color = loaderColor(selected.loaderType))
                            StatusBadge(text = "${selected.ramMb} MB", color = TextSecondary)
                        }
                        if (selected.externalPathUri.isNotBlank()) {
                            Text(
                                text = "المسار: ${selected.externalPathUri}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToMods),
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceElevated,
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Extension,
                                    contentDescription = "المودات",
                                    tint = DiamondCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "${uiState.installedMods.size} مود مضاف",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceElevated,
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Terminal,
                                    contentDescription = "المصدر",
                                    tint = RustOrange,
                                    modifier = Modifier.size(18.dp),
                                )
                                val targetLabel = runCatching {
                                    ExternalLauncherTarget.valueOf(selected.sourceLauncher).displayName
                                }.getOrDefault("لانشر خارجي")
                                Text(
                                    text = targetLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("play_minecraft_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "تشغيل",
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تشغيل النسخة الجاهزة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Scanned Ready Versions List (only shown when found)
        if (uiState.instances.isNotEmpty()) {
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                    SectionHeader(
                        title = "النسخ الجاهزة التي تم العثور عليها (${uiState.instances.size})",
                        icon = Icons.Rounded.CheckCircle,
                        accent = DiamondCyan,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.instances.forEach { inst ->
                            val isSelected = inst.id == selected?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectInstance(inst.id) }
                                    .testTag("instance_card_${inst.id}"),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else ObsidianSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) EmeraldPrimary else ObsidianBorder,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = inst.name,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Rounded.CheckCircle,
                                                    contentDescription = "محددة",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = "MC ${inst.mcVersion} • ${inst.loaderType} • ${inst.sourceLauncher}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteInstance(inst.id) },
                                        modifier = Modifier.testTag("delete_instance_${inst.id}"),
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "إزالة من القائمة",
                                            tint = TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // External Launchers Status Hub (Zalith / Mojo / Pojav)
        item {
            RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                SectionHeader(
                    title = "حالة اللانشرات الخارجية على الجهاز",
                    icon = Icons.Rounded.FolderOpen,
                    accent = RustOrange,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.externalLaunchers.forEach { launcher ->
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = launcher.name,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        StatusBadge(
                                            text = if (launcher.isInstalled) "مثبت" else "غير مثبت",
                                            color = if (launcher.isInstalled) EmeraldPrimary else TextMuted,
                                        )
                                    }
                                    Text(
                                        text = launcher.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (launcher.isInstalled) {
                                    Button(
                                        onClick = {
                                            onOpenExternalLauncher(launcher.packageName, launcher.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RustOrange),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = "فتح",
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فتح")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(launcher.downloadUrl),
                                            )
                                            context.startActivity(intent)
                                        },
                                    ) {
                                        Icon(
                                            Icons.Rounded.Download,
                                            contentDescription = "تحميل",
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تحميل")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
