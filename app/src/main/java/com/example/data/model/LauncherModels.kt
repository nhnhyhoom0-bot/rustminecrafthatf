package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class TouchButtonShape {
    CIRCLE,
    ROUNDED_RECT,
    DPAD_CLUSTER,
}

data class TouchButtonConfig(
    val id: String,
    val label: String,
    val keycode: String,
    val xPercent: Float,
    val yPercent: Float,
    val scale: Float = 1.0f,
    val opacity: Float = 0.85f,
    val shape: TouchButtonShape = TouchButtonShape.CIRCLE,
    val colorHex: Long = 0xFF22C55E,
)

object TouchControlDefaults {
    fun pvpPreset(): List<TouchButtonConfig> = listOf(
        TouchButtonConfig("dpad", "حركة", "WASD", 0.17f, 0.68f, 1.3f, 0.85f, TouchButtonShape.DPAD_CLUSTER, 0xFF38BDF8),
        TouchButtonConfig("sprint", "جري", "KEY_LCTRL", 0.17f, 0.36f, 0.9f, 0.8f, TouchButtonShape.ROUNDED_RECT, 0xFFF97316),
        TouchButtonConfig("sneak", "تسلل", "KEY_LSHIFT", 0.32f, 0.78f, 0.9f, 0.8f, TouchButtonShape.ROUNDED_RECT, 0xFFA855F7),
        TouchButtonConfig("jump", "قفز", "KEY_SPACE", 0.85f, 0.74f, 1.25f, 0.9f, TouchButtonShape.CIRCLE, 0xFF22C55E),
        TouchButtonConfig("attack", "هجوم", "MOUSE_LEFT", 0.84f, 0.45f, 1.15f, 0.9f, TouchButtonShape.CIRCLE, 0xFFEF4444),
        TouchButtonConfig("use", "استخدام", "MOUSE_RIGHT", 0.70f, 0.58f, 1.05f, 0.85f, TouchButtonShape.CIRCLE, 0xFF38BDF8),
        TouchButtonConfig("inv", "حقيبة", "KEY_E", 0.50f, 0.14f, 0.85f, 0.75f, TouchButtonShape.ROUNDED_RECT, 0xFF94A3B8),
        TouchButtonConfig("chat", "دردشة", "KEY_T", 0.62f, 0.14f, 0.85f, 0.75f, TouchButtonShape.ROUNDED_RECT, 0xFF94A3B8),
        TouchButtonConfig("f5", "كاميرا", "KEY_F5", 0.38f, 0.14f, 0.85f, 0.75f, TouchButtonShape.ROUNDED_RECT, 0xFF94A3B8),
    )

    fun builderPreset(): List<TouchButtonConfig> = listOf(
        TouchButtonConfig("dpad", "حركة", "WASD", 0.16f, 0.70f, 1.25f, 0.8f, TouchButtonShape.DPAD_CLUSTER, 0xFF38BDF8),
        TouchButtonConfig("jump", "قفز/طيران", "KEY_SPACE", 0.85f, 0.72f, 1.2f, 0.85f, TouchButtonShape.CIRCLE, 0xFF22C55E),
        TouchButtonConfig("use", "وضع بلوك", "MOUSE_RIGHT", 0.85f, 0.44f, 1.2f, 0.9f, TouchButtonShape.CIRCLE, 0xFF38BDF8),
        TouchButtonConfig("attack", "كسر", "MOUSE_LEFT", 0.70f, 0.56f, 1.05f, 0.85f, TouchButtonShape.CIRCLE, 0xFFF97316),
        TouchButtonConfig("sneak", "تسلل", "KEY_LSHIFT", 0.72f, 0.79f, 0.95f, 0.8f, TouchButtonShape.ROUNDED_RECT, 0xFFA855F7),
        TouchButtonConfig("pick", "نسخ بلوك", "MOUSE_MIDDLE", 0.50f, 0.14f, 0.9f, 0.75f, TouchButtonShape.ROUNDED_RECT, 0xFFFACC15),
        TouchButtonConfig("inv", "حقيبة", "KEY_E", 0.64f, 0.14f, 0.9f, 0.75f, TouchButtonShape.ROUNDED_RECT, 0xFF94A3B8),
    )

    fun encodeButtons(buttons: List<TouchButtonConfig>): String {
        val arr = JSONArray()
        buttons.forEach { btn ->
            val obj = JSONObject().apply {
                put("id", btn.id)
                put("label", btn.label)
                put("keycode", btn.keycode)
                put("xPercent", btn.xPercent.toDouble())
                put("yPercent", btn.yPercent.toDouble())
                put("scale", btn.scale.toDouble())
                put("opacity", btn.opacity.toDouble())
                put("shape", btn.shape.name)
                put("colorHex", btn.colorHex)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeButtons(json: String): List<TouchButtonConfig> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                TouchButtonConfig(
                    id = obj.getString("id"),
                    label = obj.getString("label"),
                    keycode = obj.getString("keycode"),
                    xPercent = obj.getDouble("xPercent").toFloat().coerceIn(0.05f, 0.95f),
                    yPercent = obj.getDouble("yPercent").toFloat().coerceIn(0.08f, 0.92f),
                    scale = obj.optDouble("scale", 1.0).toFloat().coerceIn(0.6f, 1.8f),
                    opacity = obj.optDouble("opacity", 0.85).toFloat().coerceIn(0.2f, 1.0f),
                    shape = runCatching {
                        TouchButtonShape.valueOf(obj.optString("shape", "CIRCLE"))
                    }.getOrDefault(TouchButtonShape.CIRCLE),
                    colorHex = obj.optLong("colorHex", 0xFF22C55E),
                )
            }
        }.getOrElse { pvpPreset() }
    }
}

data class MojangVersionItem(
    val id: String,
    val type: String,
    val releaseTime: String,
    val url: String,
)

data class UpdateCheckResult(
    val latestRelease: String,
    val latestSnapshot: String,
    val launcherLatestTag: String = "v1.0.0",
    val hasNewMinecraftRelease: Boolean = false,
    val checkedAtMillis: Long = System.currentTimeMillis(),
)

data class ModSearchItem(
    val projectId: String,
    val source: String, // "MODRINTH" | "CURSEFORGE"
    val title: String,
    val description: String,
    val author: String,
    val downloads: Long,
    val iconUrl: String?,
    val categories: List<String>,
    val gameVersions: List<String>,
    val loaders: List<String>,
)

data class ModVersionOption(
    val versionId: String,
    val versionNumber: String,
    val fileName: String,
    val downloadUrl: String,
    val loaders: List<String>,
    val gameVersions: List<String>,
)

data class ExternalLauncherStatus(
    val name: String,
    val packageName: String,
    val isInstalled: Boolean,
    val description: String,
    val downloadUrl: String,
)

sealed interface MicrosoftLoginState {
    data object Idle : MicrosoftLoginState
    data object RequestingCode : MicrosoftLoginState
    data class WaitingForUser(
        val userCode: String,
        val verificationUri: String,
        val expiresInSeconds: Int,
        val statusText: String,
    ) : MicrosoftLoginState
    data class Success(val username: String, val uuid: String) : MicrosoftLoginState
    data class Error(val message: String) : MicrosoftLoginState
}

data class CapeOption(
    val id: String,
    val titleAr: String,
    val primaryHex: Long,
    val accentHex: Long,
) {
    companion object {
        val ALL = listOf(
            CapeOption("EMERALD_CAPE", "وشاح الزمرد", 0xFF16A34A, 0xFF4ADE80),
            CapeOption("RUST_FORGE", "وشاح راست البرتقالي", 0xFFEA580C, 0xFFFDBA74),
            CapeOption("DIAMOND_ELITE", "وشاح الألماس", 0xFF0284C7, 0xFF7DD3FC),
            CapeOption("AMETHYST_ROYAL", "وشاح الجمشت", 0xFF9333EA, 0xFFD8B4FE),
            CapeOption("NONE", "بدون وشاح", 0xFF334155, 0xFF475569),
        )
    }
}
