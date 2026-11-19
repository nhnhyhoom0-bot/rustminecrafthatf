package com.example.core

import android.os.Build
import java.security.MessageDigest

/**
 * Bridge to the Rust Core Crate (`rust_core/src/jni_bridge.rs`).
 * Attempts to load `librustcraft_core.so` compiled via `cargo-ndk`.
 * If the native `.so` is not packaged in the pure-Gradle APK build, executes the exact
 * RFC-matched Kotlin implementation while exposing full diagnostics for the Rust crate.
 */
object RustCoreBridge {

    val isNativeRustLoaded: Boolean by lazy {
        try {
            System.loadLibrary("rustcraft_core")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    val supportedAbis: String
        get() = Build.SUPPORTED_ABIS.joinToString(", ")

    val engineModeLabel: String
        get() = if (isNativeRustLoaded) {
            "Native Rust JNI (.so loaded)"
        } else {
            "Rust Crate Spec + JVM Mirror Bridge"
        }

    // Native JNI declarations matching rust_core/src/jni_bridge.rs
    private external fun nativeGetEngineBanner(): String
    private external fun nativeGenerateOfflineUuid(username: String): String
    private external fun nativeValidateSkinDimensions(width: Int, height: Int): Boolean
    private external fun nativeBuildJvmArgs(
        ramMb: Int,
        username: String,
        uuid: String,
        mcVersion: String,
        loader: String,
        skinServer: String,
        renderer: String,
    ): String

    fun getEngineBanner(): String {
        return if (isNativeRustLoaded) {
            runCatching { nativeGetEngineBanner() }
                .getOrDefault("RustCraft Core Engine v1.0.0 (JNI)")
        } else {
            "RustCraft Core Engine v1.0.0 (rust_core/ • Kotlin Bridge)"
        }
    }

    /**
     * Generates a standard Minecraft OfflinePlayer UUIDv3 (`OfflinePlayer:<username>`).
     */
    fun generateOfflineUuid(username: String): String {
        val clean = username.trim().ifEmpty { "Player" }
        if (isNativeRustLoaded) {
            runCatching { return nativeGenerateOfflineUuid(clean) }
        }
        val md5 = MessageDigest.getInstance("MD5")
        val bytes = md5.digest("OfflinePlayer:$clean".toByteArray(Charsets.UTF_8))
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        return buildString {
            bytes.forEachIndexed { idx, b ->
                append("%02x".format(b))
                if (idx == 3 || idx == 5 || idx == 7 || idx == 9) append('-')
            }
        }
    }

    fun validateSkinDimensions(width: Int, height: Int): Boolean {
        if (isNativeRustLoaded) {
            runCatching { return nativeValidateSkinDimensions(width, height) }
        }
        return (width == 64 && height == 64) ||
            (width == 64 && height == 32) ||
            (width == 128 && height == 128)
    }

    fun buildJvmLaunchArgs(
        ramMb: Int,
        username: String,
        uuid: String,
        mcVersion: String,
        loader: String,
        skinServerUrl: String?,
        renderer: String,
    ): List<String> {
        if (isNativeRustLoaded) {
            runCatching {
                val raw = nativeBuildJvmArgs(
                    ramMb,
                    username,
                    uuid,
                    mcVersion,
                    loader,
                    skinServerUrl.orEmpty(),
                    renderer,
                )
                return raw.split(" ").filter { it.isNotBlank() }
            }
        }
        val args = mutableListOf(
            "-Xms${(ramMb / 2).coerceAtLeast(512)}M",
            "-Xmx${ramMb.coerceAtLeast(1024)}M",
            "-XX:+UseG1GC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:MaxGCPauseMillis=200",
            "-Dorg.lwjgl.opengl.libname=$renderer",
        )
        if (!skinServerUrl.isNullOrBlank()) {
            args.add("-Dauthlibinjector.api=${skinServerUrl.trim()}")
        }
        args.add("--username=$username")
        args.add("--uuid=$uuid")
        args.add("--version=$mcVersion")
        args.add("--loader=$loader")
        return args
    }
}
