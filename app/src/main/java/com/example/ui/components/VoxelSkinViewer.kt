package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.SkinModel
import com.example.data.model.CapeOption
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import java.io.File

@Composable
fun VoxelSkinViewer(
    skinPath: String,
    skinModel: String,
    capeStyle: String,
    showBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(skinPath) {
        if (skinPath.isNotBlank() && File(skinPath).exists()) {
            runCatching { BitmapFactory.decodeFile(skinPath) }.getOrNull()
        } else {
            null
        }
    }
    val isSlim = skinModel == SkinModel.SLIM.name
    val cape = remember(capeStyle) {
        CapeOption.ALL.firstOrNull { it.id == capeStyle } ?: CapeOption.ALL.first()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F2D3D), Color(0xFF0D131C)),
                )
            )
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val gridStep = size.width / 8f
            for (i in 0..8) {
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(i * gridStep, 0f),
                    end = Offset(i * gridStep, size.height),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(0f, i * gridStep),
                    end = Offset(size.width, i * gridStep),
                    strokeWidth = 1f,
                )
            }

            // Ground shadow
            val unit = (size.height * 0.78f) / 32f
            val centerX = size.width / 2f
            val topY = size.height * 0.11f

            drawOval(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(centerX - 8f * unit, topY + 30.8f * unit),
                size = Size(16f * unit, 3.2f * unit),
            )

            // Draw Cape behind body (front view peeks slightly, back view full)
            if (cape.id != "NONE" && showBack) {
                val capeW = 10f * unit
                val capeH = 16f * unit
                val capeX = centerX - capeW / 2f
                val capeY = topY + 8.5f * unit
                drawRect(
                    color = Color(cape.primaryHex),
                    topLeft = Offset(capeX, capeY),
                    size = Size(capeW, capeH),
                )
                drawRect(
                    color = Color(cape.accentHex),
                    topLeft = Offset(capeX + 2f * unit, capeY + 3f * unit),
                    size = Size(6f * unit, 6f * unit),
                )
            }

            if (bitmap != null) {
                drawSkinFromBitmap(
                    bmp = bitmap,
                    centerX = centerX,
                    topY = topY,
                    unit = unit,
                    isSlim = isSlim,
                    showBack = showBack,
                )
            } else {
                drawDefaultRustSkin(
                    centerX = centerX,
                    topY = topY,
                    unit = unit,
                    isSlim = isSlim,
                    showBack = showBack,
                )
            }

            // Subtle cape peek in front view
            if (cape.id != "NONE" && !showBack) {
                drawRect(
                    color = Color(cape.primaryHex).copy(alpha = 0.85f),
                    topLeft = Offset(centerX - 4.5f * unit, topY + 19f * unit),
                    size = Size(9f * unit, 2f * unit),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelRectFromBitmap(
    bmp: Bitmap,
    srcX: Int,
    srcY: Int,
    srcW: Int,
    srcH: Int,
    dstX: Float,
    dstY: Float,
    unit: Float,
    skipTransparent: Boolean = false,
) {
    val scaleX = bmp.width / 64f
    val scaleY = bmp.height / (if (bmp.height == 32 && bmp.width == 64) 32f else 64f)
    for (py in 0 until srcH) {
        for (px in 0 until srcW) {
            val sx = ((srcX + px) * scaleX).toInt().coerceIn(0, bmp.width - 1)
            val sy = ((srcY + py) * scaleY).toInt().coerceIn(0, bmp.height - 1)
            val pixel = bmp.getPixel(sx, sy)
            val alpha = (pixel ushr 24) and 0xFF
            if (skipTransparent && alpha < 25) continue
            if (alpha == 0) continue
            drawRect(
                color = Color(pixel),
                topLeft = Offset(dstX + px * unit, dstY + py * unit),
                size = Size(unit + 0.5f, unit + 0.5f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkinFromBitmap(
    bmp: Bitmap,
    centerX: Float,
    topY: Float,
    unit: Float,
    isSlim: Boolean,
    showBack: Boolean,
) {
    val isLegacy = bmp.height == 32 && bmp.width == 64
    val armW = if (isSlim) 3 else 4
    val headX = centerX - 4f * unit
    val bodyX = centerX - 4f * unit
    val bodyY = topY + 8f * unit
    val legY = topY + 20f * unit

    // Head + Hat Layer
    val headSrcX = if (showBack) 24 else 8
    val hatSrcX = if (showBack) 56 else 40
    drawPixelRectFromBitmap(bmp, headSrcX, 8, 8, 8, headX, topY, unit)
    drawPixelRectFromBitmap(bmp, hatSrcX, 8, 8, 8, headX, topY, unit, skipTransparent = true)

    // Torso
    val torsoSrcX = if (showBack) 32 else 20
    drawPixelRectFromBitmap(bmp, torsoSrcX, 20, 8, 12, bodyX, bodyY, unit)
    if (!isLegacy) {
        drawPixelRectFromBitmap(bmp, torsoSrcX, 36, 8, 12, bodyX, bodyY, unit, skipTransparent = true)
    }

    // Arms
    val armSrcX = if (showBack) 52 else 44
    drawPixelRectFromBitmap(bmp, armSrcX, 20, armW, 12, bodyX - armW * unit, bodyY, unit)
    if (!isLegacy) {
        val leftArmSrcX = if (showBack) 44 else 36
        drawPixelRectFromBitmap(bmp, leftArmSrcX, 52, armW, 12, bodyX + 8f * unit, bodyY, unit)
    } else {
        drawPixelRectFromBitmap(bmp, armSrcX, 20, armW, 12, bodyX + 8f * unit, bodyY, unit)
    }

    // Legs
    val legSrcX = if (showBack) 12 else 4
    drawPixelRectFromBitmap(bmp, legSrcX, 20, 4, 12, bodyX, legY, unit)
    if (!isLegacy) {
        val leftLegSrcX = if (showBack) 28 else 20
        drawPixelRectFromBitmap(bmp, leftLegSrcX, 52, 4, 12, bodyX + 4f * unit, legY, unit)
    } else {
        drawPixelRectFromBitmap(bmp, legSrcX, 20, 4, 12, bodyX + 4f * unit, legY, unit)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDefaultRustSkin(
    centerX: Float,
    topY: Float,
    unit: Float,
    isSlim: Boolean,
    showBack: Boolean,
) {
    val armW = if (isSlim) 3f else 4f
    val headX = centerX - 4f * unit
    val bodyX = centerX - 4f * unit
    val bodyY = topY + 8f * unit
    val legY = topY + 20f * unit

    // Head base
    drawRect(Color(0xFFD6A37A), Offset(headX, topY), Size(8f * unit, 8f * unit))
    // Hair / Hood
    drawRect(Color(0xFF1F2937), Offset(headX, topY), Size(8f * unit, 2.5f * unit))
    if (!showBack) {
        // Emerald Visor Eyes
        drawRect(Color(0xFF22C55E), Offset(headX + 1.2f * unit, topY + 4f * unit), Size(1.8f * unit, 1.2f * unit))
        drawRect(Color(0xFF22C55E), Offset(headX + 5f * unit, topY + 4f * unit), Size(1.8f * unit, 1.2f * unit))
        drawRect(Color(0xFF9E6B47), Offset(headX + 3f * unit, topY + 6f * unit), Size(2f * unit, 1f * unit))
    } else {
        drawRect(Color(0xFF1F2937), Offset(headX, topY), Size(8f * unit, 6.5f * unit))
    }

    // Torso (Obsidian + Rust-Orange Jacket + Emerald Core)
    drawRect(Color(0xFF1E293B), Offset(bodyX, bodyY), Size(8f * unit, 12f * unit))
    drawRect(Color(0xFFF97316), Offset(bodyX + 1f * unit, bodyY + 1f * unit), Size(6f * unit, 8f * unit))
    if (!showBack) {
        drawRect(Color(0xFF22C55E), Offset(bodyX + 3f * unit, bodyY + 3f * unit), Size(2f * unit, 2.5f * unit))
    }

    // Arms
    drawRect(Color(0xFF334155), Offset(bodyX - armW * unit, bodyY), Size(armW * unit, 8f * unit))
    drawRect(Color(0xFFD6A37A), Offset(bodyX - armW * unit, bodyY + 8f * unit), Size(armW * unit, 4f * unit))
    drawRect(Color(0xFF334155), Offset(bodyX + 8f * unit, bodyY), Size(armW * unit, 8f * unit))
    drawRect(Color(0xFFD6A37A), Offset(bodyX + 8f * unit, bodyY + 8f * unit), Size(armW * unit, 4f * unit))

    // Legs
    drawRect(Color(0xFF0F172A), Offset(bodyX, legY), Size(3.9f * unit, 10f * unit))
    drawRect(Color(0xFF0F172A), Offset(bodyX + 4.1f * unit, legY), Size(3.9f * unit, 10f * unit))
    // Boots
    drawRect(Color(0xFFF97316), Offset(bodyX, legY + 10f * unit), Size(3.9f * unit, 2f * unit))
    drawRect(Color(0xFFF97316), Offset(bodyX + 4.1f * unit, legY + 10f * unit), Size(3.9f * unit, 2f * unit))
}

@Composable
fun SkinHeadAvatar(
    skinPath: String,
    size: Dp = 44.dp,
) {
    val bitmap = remember(skinPath) {
        if (skinPath.isNotBlank() && File(skinPath).exists()) {
            runCatching { BitmapFactory.decodeFile(skinPath) }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val unit = this.size.width / 8f
            if (bitmap != null) {
                drawPixelRectFromBitmap(bitmap, 8, 8, 8, 8, 0f, 0f, unit)
                drawPixelRectFromBitmap(bitmap, 40, 8, 8, 8, 0f, 0f, unit, skipTransparent = true)
            } else {
                drawRect(Color(0xFFD6A37A), Offset.Zero, this.size)
                drawRect(Color(0xFF1F2937), Offset.Zero, Size(this.size.width, 2.5f * unit))
                drawRect(Color(0xFF22C55E), Offset(1.2f * unit, 4f * unit), Size(1.8f * unit, 1.3f * unit))
                drawRect(Color(0xFF22C55E), Offset(5f * unit, 4f * unit), Size(1.8f * unit, 1.3f * unit))
            }
        }
    }
}
