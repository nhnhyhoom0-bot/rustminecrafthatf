package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.TouchButtonShape
import com.example.data.model.TouchControlDefaults
import com.example.ui.LauncherUiState
import com.example.ui.components.SkinHeadAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RustOrange
import kotlin.math.roundToInt

@Composable
fun PlayOverlayScreen(
    uiState: LauncherUiState,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val haptic = LocalHapticFeedback.current
    val activeLayout = uiState.activeTouchLayout
    val buttons = remember(activeLayout?.buttonsJson) {
        activeLayout?.let { TouchControlDefaults.decodeButtons(it.buttonsJson) }
            ?: TouchControlDefaults.pvpPreset()
    }
    var selectedHotbarSlot by remember { mutableIntStateOf(0) }
    var lastPressedAction by remember { mutableStateOf("جاهز - المس الأزرار للتجربة") }
    var showConsole by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("play_sandbox_overlay"),
    ) {
        // Stylized Voxel Horizon Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A192F), Color(0xFF132D46), Color(0xFF1A3A2A)),
                ),
            )
            // Voxel Sun / Moon
            drawRect(
                color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                topLeft = Offset(size.width * 0.72f, size.height * 0.14f),
                size = Size(54f, 54f),
            )
            // Horizon Terrain Blocks
            val blockW = size.width / 12f
            for (i in 0..11) {
                val h = size.height * (0.28f + (i % 3) * 0.04f)
                drawRect(
                    color = if (i % 2 == 0) Color(0xFF166534) else Color(0xFF15803D),
                    topLeft = Offset(i * blockW, size.height - h),
                    size = Size(blockW + 2f, 22f),
                )
                drawRect(
                    color = Color(0xFF451A03),
                    topLeft = Offset(i * blockW, size.height - h + 22f),
                    size = Size(blockW + 2f, h),
                )
            }
            // Crosshair
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - 14f, cy), Offset(cx + 14f, cy), 3f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(cx, cy - 14f), Offset(cx, cy + 14f), 3f)
        }

        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkinHeadAvatar(skinPath = uiState.activeAccount?.skinUri.orEmpty(), size = 36.dp)
                Column {
                    Text(
                        text = "${uiState.activeAccount?.username ?: "RustBuilder"} • ${uiState.selectedInstance?.mcVersion ?: "1.21.4"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                    Text(
                        text = lastPressedAction,
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusBadge(text = "60 FPS", color = EmeraldPrimary)
                StatusBadge(text = "${uiState.selectedInstance?.ramMb ?: 2560}MB", color = DiamondCyan)
                IconButton(
                    onClick = { showConsole = !showConsole },
                    modifier = Modifier.testTag("toggle_console_button"),
                ) {
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = "سجل التشغيل",
                        tint = if (showConsole) RustOrange else Color.White,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("exit_play_overlay_button"),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "إغلاق المحاكي",
                        tint = Color.White,
                    )
                }
            }
        }

        // Expandable Console Drawer
        if (showConsole) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, ObsidianBorder),
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.launchLogs) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                        )
                    }
                }
            }
        }

        // Interactive Touch Controls Layer from Active Layout
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            buttons.forEach { btn ->
                val baseDp = if (btn.shape == TouchButtonShape.DPAD_CLUSTER) 76.dp else 54.dp
                val btnSizeDp = baseDp * btn.scale
                val btnSizePx = with(density) { btnSizeDp.toPx() }
                val xPos = (btn.xPercent * widthPx - btnSizePx / 2f)
                    .coerceIn(0f, (widthPx - btnSizePx).coerceAtLeast(0f))
                val yPos = (btn.yPercent * heightPx - btnSizePx / 2f)
                    .coerceIn(0f, (heightPx - btnSizePx).coerceAtLeast(0f))

                var pressed by remember { mutableStateOf(false) }
                val btnColor = Color(btn.colorHex)
                val shape = when (btn.shape) {
                    TouchButtonShape.CIRCLE -> CircleShape
                    TouchButtonShape.ROUNDED_RECT -> RoundedCornerShape(14.dp)
                    TouchButtonShape.DPAD_CLUSTER -> CircleShape
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPos.roundToInt(), yPos.roundToInt()) }
                        .size(btnSizeDp)
                        .alpha(if (pressed) 1f else btn.opacity)
                        .clip(shape)
                        .background(
                            if (pressed) btnColor.copy(alpha = 0.6f) else btnColor.copy(alpha = 0.22f)
                        )
                        .border(2.dp, if (pressed) Color.White else btnColor, shape)
                        .pointerInput(btn.id) {
                            detectTapGestures(
                                onPress = {
                                    pressed = true
                                    if (activeLayout?.hapticEnabled != false) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    lastPressedAction = "أمر: ${btn.label} (${btn.keycode})"
                                    tryAwaitRelease()
                                    pressed = false
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = btn.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Interactive Hotbar 1-9
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(9) { slot ->
                val isSelected = slot == selectedHotbarSlot
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) EmeraldPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clickable {
                            selectedHotbarSlot = slot
                            lastPressedAction = "تحديد الخانة ${slot + 1}"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${slot + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
