package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountType
import com.example.data.local.SkinModel
import com.example.data.model.CapeOption
import com.example.data.model.MicrosoftLoginState
import com.example.ui.LauncherUiState
import com.example.ui.components.RustCraftCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkinHeadAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoxelSkinViewer
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RustOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun SkinsAccountsScreen(
    uiState: LauncherUiState,
    onImportSkinUri: (Uri) -> Unit,
    onFetchSkinByUsername: (String) -> Unit,
    onUpdateSkinModel: (String) -> Unit,
    onUpdateCape: (String) -> Unit,
    onUpdateSkinServer: (String) -> Unit,
    onSyncSkinToServers: () -> Unit,
    onAddOfflineAccount: (String, String) -> Unit,
    onActivateAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onStartMicrosoftLogin: () -> Unit,
    onResetMicrosoftLogin: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showBackView by remember { mutableStateOf(false) }
    var usernameLookup by remember { mutableStateOf("") }
    var showAddCrackedDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Zero-permission Android Photo Picker for custom Minecraft Skin PNG
    val skinPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onImportSkinUri(uri)
    }

    val active = uiState.activeAccount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Box(modifier = Modifier.widthIn(max = 680.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("تخصيص السكن والسيرفرات", "الحسابات (كراك / مايكروسوفت)").forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedSubTab == idx,
                            onClick = { selectedSubTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                            modifier = Modifier.testTag("skins_subtab_$idx"),
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 0) {
            // Interactive 2D/Isometric Skin Viewer Card
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                    SectionHeader(
                        title = "معاينة السكن التفاعلية",
                        icon = Icons.Rounded.AccountCircle,
                        accent = EmeraldPrimary,
                        trailing = {
                            OutlinedButton(
                                onClick = { showBackView = !showBackView },
                                modifier = Modifier.testTag("flip_skin_button"),
                            ) {
                                Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "تدوير", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showBackView) "الأمام" else "الخلف والوشاح")
                            }
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VoxelSkinViewer(
                            skinPath = active?.skinUri.orEmpty(),
                            skinModel = active?.skinModel ?: SkinModel.CLASSIC.name,
                            capeStyle = active?.capeStyle ?: "EMERALD_CAPE",
                            showBack = showBackView,
                            modifier = Modifier
                                .width(155.dp)
                                .height(225.dp),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = active?.username ?: "RustBuilder",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            StatusBadge(
                                text = if (active?.accountType == AccountType.MICROSOFT.name) "أصلي مايكروسوفت" else "كراك (Offline)",
                                color = if (active?.accountType == AccountType.MICROSOFT.name) EmeraldPrimary else RustOrange,
                            )

                            Text("نوع اليدين (النموذج):", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = (active?.skinModel ?: SkinModel.CLASSIC.name) == SkinModel.CLASSIC.name,
                                    onClick = { onUpdateSkinModel(SkinModel.CLASSIC.name) },
                                    label = { Text("Classic") },
                                )
                                FilterChip(
                                    selected = active?.skinModel == SkinModel.SLIM.name,
                                    onClick = { onUpdateSkinModel(SkinModel.SLIM.name) },
                                    label = { Text("Slim") },
                                )
                            }

                            Button(
                                onClick = {
                                    skinPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_skin_photo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختيار سكن PNG")
                            }
                        }
                    }

                    // Fetch skin by username
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = usernameLookup,
                            onValueChange = { usernameLookup = it },
                            label = { Text("جلب سكن لاعب أونلاين بالاسم") },
                            placeholder = { Text("مثال: Notch أو Dream") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { onFetchSkinByUsername(usernameLookup) },
                            modifier = Modifier.height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "جلب السكن")
                        }
                    }
                }
            }

            // Capes Selection
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                    SectionHeader(
                        title = "تخصيص الوشاح (Cape)",
                        icon = Icons.Rounded.Public,
                        accent = DiamondCyan,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CapeOption.ALL) { cape ->
                            FilterChip(
                                selected = (active?.capeStyle ?: "EMERALD_CAPE") == cape.id,
                                onClick = { onUpdateCape(cape.id) },
                                label = { Text(cape.titleAr) },
                            )
                        }
                    }
                }
            }

            // Server Skin Visibility Card
            item {
                var customServerInput by remember(active?.id, active?.skinServerUrl) {
                    mutableStateOf(active?.skinServerUrl ?: "https://authserver.ely.by/api/authlib-injector")
                }

                RustCraftCard(
                    modifier = Modifier.widthIn(max = 680.dp),
                    borderColor = DiamondCyan.copy(alpha = 0.45f),
                ) {
                    SectionHeader(
                        title = "إظهار السكن في السيرفرات (Multiplayer)",
                        icon = Icons.Rounded.CloudUpload,
                        accent = EmeraldPrimary,
                    )
                    Text(
                        text = "للحسابات الأصلية يتم رفع السكن مباشرة إلى خوادم Mojang. ولحسابات الكراك يتم حقن Authlib-Injector (مثل Ely.by أو LittleSkin) ليظهر سكنك ووشاحك للاعبين داخل السيرفرات الداعمة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )

                    val presets = listOf(
                        "https://authserver.ely.by/api/authlib-injector" to "Ely.by (الأشهر)",
                        "https://littleskin.cn/api/yggdrasil" to "LittleSkin",
                        "https://api.minecraftservices.com" to "Mojang الرسمي",
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { (url, label) ->
                            FilterChip(
                                selected = customServerInput == url,
                                onClick = {
                                    customServerInput = url
                                    onUpdateSkinServer(url)
                                },
                                label = { Text(label) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customServerInput,
                        onValueChange = { customServerInput = it },
                        label = { Text("رابط خادم السكنات (Authlib-Injector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateSkinServer(customServerInput) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("حفظ الخادم")
                        }
                        Button(
                            onClick = onSyncSkinToServers,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_skin_server_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مزامنة للسيرفرات")
                        }
                    }
                }
            }
        } else {
            // Accounts Tab: Microsoft OAuth + Cracked Accounts
            item {
                RustCraftCard(
                    modifier = Modifier.widthIn(max = 680.dp),
                    borderColor = EmeraldPrimary.copy(alpha = 0.45f),
                ) {
                    SectionHeader(
                        title = "إضافة حساب مايكروسوفت أصلي",
                        icon = Icons.Rounded.Login,
                        accent = EmeraldPrimary,
                    )

                    when (val ms = uiState.microsoftLoginState) {
                        is MicrosoftLoginState.Idle -> {
                            Text(
                                text = "سجّل الدخول بحساب مايكروسوفت عبر رمز الجهاز الآمن (Device Code OAuth2 + Xbox Live + XSTS) للعب في السيرفرات الرسمية مثل Hypixel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Button(
                                onClick = onStartMicrosoftLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("microsoft_login_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            ) {
                                Icon(Icons.Rounded.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تسجيل الدخول عبر Microsoft", fontWeight = FontWeight.Bold)
                            }
                        }
                        is MicrosoftLoginState.RequestingCode -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = EmeraldPrimary)
                                Text("جارٍ طلب رمز التحقق من مايكروسوفت...")
                            }
                        }
                        is MicrosoftLoginState.WaitingForUser -> {
                            Text(ms.statusText, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianSurfaceElevated,
                                border = BorderStroke(1.dp, EmeraldPrimary),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = ms.userCode,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = EmeraldPrimary,
                                    )
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(ms.userCode))
                                            onShowMessage("تم نسخ الرمز: ${ms.userCode}")
                                        },
                                    ) {
                                        Text("نسخ الرمز")
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ms.verificationUri))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح رابط التحقق")
                                }
                                OutlinedButton(onClick = onResetMicrosoftLogin) {
                                    Text("إلغاء")
                                }
                            }
                        }
                        is MicrosoftLoginState.Success -> {
                            Text(
                                text = "تم توثيق الحساب: ${ms.username}",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            OutlinedButton(onClick = onResetMicrosoftLogin) {
                                Text("إضافة حساب آخر")
                            }
                        }
                        is MicrosoftLoginState.Error -> {
                            Text(text = ms.message, color = RustOrange, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = onResetMicrosoftLogin) {
                                Text("حسناً")
                            }
                        }
                    }
                }
            }

            // Cracked / Saved Accounts List
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 680.dp)) {
                    SectionHeader(
                        title = "الحسابات المحفوظة (${uiState.accounts.size})",
                        icon = Icons.Rounded.AccountCircle,
                        accent = RustOrange,
                        trailing = {
                            TextButton(
                                onClick = { showAddCrackedDialog = true },
                                modifier = Modifier.testTag("add_cracked_account_button"),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حساب كراك")
                            }
                        },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.accounts.forEach { acc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onActivateAccount(acc.id) },
                                color = if (acc.isActive) EmeraldPrimary.copy(alpha = 0.12f) else ObsidianSurfaceElevated,
                                border = BorderStroke(1.dp, if (acc.isActive) EmeraldPrimary else ObsidianBorder),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    SkinHeadAvatar(skinPath = acc.skinUri, size = 42.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(acc.username, style = MaterialTheme.typography.titleMedium)
                                            if (acc.isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Rounded.CheckCircle,
                                                    contentDescription = "نشط",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${if (acc.accountType == AccountType.MICROSOFT.name) "مايكروسوفت أصلي" else "كراك"} • ${acc.uuid.take(14)}...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                        )
                                    }
                                    if (uiState.accounts.size > 1) {
                                        IconButton(onClick = { onDeleteAccount(acc.id) }) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "حذف الحساب",
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
        }
    }

    if (showAddCrackedDialog) {
        var nameInput by remember { mutableStateOf("") }
        var skinServer by remember { mutableStateOf(uiState.settings.defaultSkinServer) }
        AlertDialog(
            onDismissRequest = { showAddCrackedDialog = false },
            title = { Text("إضافة حساب كراك (Offline)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم اللاعب (Username)") },
                        placeholder = { Text("مثال: ArabCraft99") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = skinServer,
                        onValueChange = { skinServer = it },
                        label = { Text("خادم السكنات للسيرفرات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onAddOfflineAccount(nameInput, skinServer)
                            showAddCrackedDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCrackedDialog = false }) { Text("إلغاء") }
            },
        )
    }
}
