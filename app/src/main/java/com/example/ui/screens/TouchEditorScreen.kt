package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.TouchLayoutEntity
import com.example.data.model.TouchButtonConfig
import com.example.data.model.TouchButtonShape
import com.example.data.model.TouchControlDefaults
import com.example.ui.LauncherUiState
import com.example.ui.components.RustCraftCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RustOrange
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TouchEditorScreen(
    uiState: LauncherUiState,
    onActivateLayout: (Long) -> Unit,
    onCreateLayout: (String) -> Unit,
    onSaveButtons: (TouchLayoutEntity, List<TouchButtonConfig>) -> Unit,
    onUpdateLayoutPrefs: (TouchLayoutEntity, Float, Boolean) -> Unit,
) {
    val activeLayout = uiState.activeTouchLayout ?: uiState.touchLayouts.firstOrNull()
    var editableButtons by remember(activeLayout?.id) {
        mutableStateOf(
            activeLayout?.let { TouchControlDefaults.decodeButtons(it.buttonsJson) }
                ?: TouchControlDefaults.pvpPreset()
        )
    }
    LaunchedEffect(activeLayout?.buttonsJson) {
        if (activeLayout != null) {
            editableButtons = TouchControlDefaults.decodeButtons(activeLayout.buttonsJson)
        }
    }

    var selectedButtonId by remember { mutableStateOf<String?>(editableButtons.firstOrNull()?.id) }
    var showNewLayoutDialog by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }

    val selectedBtn = editableButtons.firstOrNull { it.id == selectedButtonId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Layout selector & presets
        item {
            RustCraftCard(modifier = Modifier.widthIn(max = 720.dp)) {
                SectionHeader(
                    title = "محرر أزرار التحكم باللمس",
                    icon = Icons.Rounded.Gamepad,
                    accent = DiamondCyan,
                    trailing = {
                        TextButton(
                            onClick = { showNewLayoutDialog = true },
                            modifier = Modifier.testTag("new_touch_layout_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تخطيط جديد")
                        }
                    },
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.touchLayouts, key = { it.id }) { layout ->
                        FilterChip(
                            selected = layout.id == activeLayout?.id,
                            onClick = { onActivateLayout(layout.id) },
                            label = { Text(layout.name) },
                        )
                    }
                }

                Text(
                    text = "اسحب أي زر داخل شاشة المحاكاة لتغيير موقعه، واضغط عليه لتعديل حجمه وشفافيته واختصاره:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        // Interactive Drag & Drop Canvas
        item {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .height(275.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0E1B2B), Color(0xFF152A22), Color(0xFF0B111A)),
                        )
                    )
                    .border(1.5.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                    .testTag("touch_editor_canvas"),
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }

                // Subtle grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 8
                    val rows = 5
                    for (c in 1 until cols) {
                        val x = size.width * c / cols
                        drawLine(Color.White.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height))
                    }
                    for (r in 1 until rows) {
                        val y = size.height * r / rows
                        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y))
                    }
                    // Crosshair center
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 4f,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                }

                // Hotbar preview at bottom center
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(9) { slot ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(
                                    1.dp,
                                    if (slot == 0) EmeraldPrimary else Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }

                // Draggable Buttons
                editableButtons.forEach { btn ->
                    val baseDp = if (btn.shape == TouchButtonShape.DPAD_CLUSTER) 62.dp else 46.dp
                    val btnSizeDp = baseDp * btn.scale
                    val btnSizePx = with(density) { btnSizeDp.toPx() }
                    val isSelected = btn.id == selectedButtonId

                    val centerX = (btn.xPercent * widthPx - btnSizePx / 2f)
                        .coerceIn(0f, (widthPx - btnSizePx).coerceAtLeast(0f))
                    val centerY = (btn.yPercent * heightPx - btnSizePx / 2f)
                        .coerceIn(0f, (heightPx - btnSizePx).coerceAtLeast(0f))

                    val btnColor = Color(btn.colorHex)
                    val shape = when (btn.shape) {
                        TouchButtonShape.CIRCLE -> CircleShape
                        TouchButtonShape.ROUNDED_RECT -> RoundedCornerShape(12.dp)
                        TouchButtonShape.DPAD_CLUSTER -> CircleShape
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(centerX.roundToInt(), centerY.roundToInt()) }
                            .size(btnSizeDp)
                            .alpha(btn.opacity)
                            .clip(shape)
                            .background(btnColor.copy(alpha = 0.24f))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.2.dp,
                                color = if (isSelected) Color.White else btnColor,
                                shape = shape,
                            )
                            .pointerInput(btn.id, widthPx, heightPx) {
                                detectTapGestures { selectedButtonId = btn.id }
                            }
                            .pointerInput(btn.id, widthPx, heightPx) {
                                detectDragGestures(
                                    onDragStart = { selectedButtonId = btn.id },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        editableButtons = editableButtons.map { item ->
                                            if (item.id == btn.id) {
                                                val nx = (item.xPercent + dragAmount.x / widthPx).coerceIn(0.06f, 0.94f)
                                                val ny = (item.yPercent + dragAmount.y / heightPx).coerceIn(0.08f, 0.90f)
                                                item.copy(xPercent = nx, yPercent = ny)
                                            } else {
                                                item
                                            }
                                        }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = btn.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Selected Button Inspector + Actions
        if (selectedBtn != null) {
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 720.dp)) {
                    SectionHeader(
                        title = "تعديل الزر: ${selectedBtn.label} (${selectedBtn.keycode})",
                        icon = Icons.Rounded.Tune,
                        accent = EmeraldPrimary,
                    )

                    Text(
                        text = "حجم الزر: ${"%.0f".format(selectedBtn.scale * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = selectedBtn.scale,
                        onValueChange = { scale ->
                            editableButtons = editableButtons.map {
                                if (it.id == selectedBtn.id) it.copy(scale = scale) else it
                            }
                        },
                        valueRange = 0.65f..1.65f,
                    )

                    Text(
                        text = "الشفافية: ${"%.0f".format(selectedBtn.opacity * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = selectedBtn.opacity,
                        onValueChange = { opacity ->
                            editableButtons = editableButtons.map {
                                if (it.id == selectedBtn.id) it.copy(opacity = opacity) else it
                            }
                        },
                        valueRange = 0.25f..1.0f,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showAddButtonDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة زر")
                        }
                        OutlinedButton(
                            onClick = {
                                editableButtons = TouchControlDefaults.pvpPreset()
                                selectedButtonId = editableButtons.firstOrNull()?.id
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة الافتراضي")
                        }
                    }
                }
            }
        }

        // Global Preferences + Save Button
        if (activeLayout != null) {
            item {
                RustCraftCard(modifier = Modifier.widthIn(max = 720.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("اهتزاز لمسي عند الضغط (Haptic)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "يعطي استجابة فورية أثناء القتال والبناء",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = activeLayout.hapticEnabled,
                            onCheckedChange = {
                                onUpdateLayoutPrefs(activeLayout, activeLayout.globalOpacity, it)
                            },
                        )
                    }

                    Button(
                        onClick = { onSaveButtons(activeLayout, editableButtons) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_touch_layout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ تخطيط اللمس", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewLayoutDialog) {
        var layoutName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewLayoutDialog = false },
            title = { Text("تخطيط لمس جديد") },
            text = {
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("اسم التخطيط") },
                    placeholder = { Text("مثال: تحكم سكاي وارز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateLayout(layoutName.ifBlank { "تخطيط مخصص" })
                        showNewLayoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewLayoutDialog = false }) { Text("إلغاء") }
            },
        )
    }

    if (showAddButtonDialog) {
        var labelInput by remember { mutableStateOf("") }
        var keycodeInput by remember { mutableStateOf("KEY_Q") }
        AlertDialog(
            onDismissRequest = { showAddButtonDialog = false },
            title = { Text("إضافة زر تحكم مخصص") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("اسم الزر") },
                        placeholder = { Text("مثال: رمي (Q)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = keycodeInput,
                        onValueChange = { keycodeInput = it },
                        label = { Text("الاختصار (Keycode)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBtn = TouchButtonConfig(
                            id = "custom_${System.currentTimeMillis()}",
                            label = labelInput.ifBlank { "زر" },
                            keycode = keycodeInput.ifBlank { "KEY_Q" },
                            xPercent = 0.5f,
                            yPercent = 0.5f,
                            shape = TouchButtonShape.ROUNDED_RECT,
                            colorHex = 0xFFF97316,
                        )
                        editableButtons = editableButtons + newBtn
                        selectedButtonId = newBtn.id
                        showAddButtonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RustOrange),
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddButtonDialog = false }) { Text("إلغاء") }
            },
        )
    }
}
