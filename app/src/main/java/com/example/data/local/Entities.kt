package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    OFFLINE,
    MICROSOFT,
}

enum class SkinModel {
    CLASSIC,
    SLIM,
}

enum class LoaderType(val displayName: String, val slug: String) {
    VANILLA("Vanilla", "vanilla"),
    FABRIC("Fabric", "fabric"),
    FORGE("Forge", "forge"),
    NEOFORGE("NeoForge", "neoforge"),
    QUILT("Quilt", "quilt"),
}

enum class ExternalLauncherTarget(val displayName: String, val packageName: String?) {
    RUSTCRAFT_SANDBOX("محاكي اللمس الداخلي (RustCraft)", null),
    ZALITH("Zalith Launcher", "com.movtery.zalithlauncher"),
    MOJO("MojoLauncher", "git.artdeell.mojo"),
    POJAV("PojavLauncher", "net.kdt.pojavlaunch"),
}

enum class ModSource {
    MODRINTH,
    CURSEFORGE,
    LOCAL,
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val uuid: String,
    val accountType: String = AccountType.OFFLINE.name,
    val accessToken: String = "",
    val refreshToken: String = "",
    val skinUri: String = "",
    val skinModel: String = SkinModel.CLASSIC.name,
    val capeStyle: String = "EMERALD_CAPE",
    val skinServerUrl: String = "https://authserver.ely.by/api/authlib-injector",
    val isActive: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "instances")
data class InstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mcVersion: String,
    val loaderType: String = LoaderType.FABRIC.name,
    val loaderVersion: String = "0.16.10",
    val ramMb: Int = 2048,
    val jvmArgs: String = "-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions",
    val sourceLauncher: String = ExternalLauncherTarget.RUSTCRAFT_SANDBOX.name,
    val externalPathUri: String = "",
    val isReady: Boolean = true,
    val isSelected: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "installed_mods")
data class InstalledModEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instanceId: Long,
    val projectId: String,
    val source: String = ModSource.MODRINTH.name,
    val title: String,
    val author: String,
    val versionNumber: String,
    val iconUrl: String = "",
    val fileName: String,
    val downloadUrl: String = "",
    val isEnabled: Boolean = true,
    val mcVersion: String,
    val loaderType: String,
    val installedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "touch_layouts")
data class TouchLayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = false,
    val buttonsJson: String,
    val globalOpacity: Float = 0.85f,
    val hapticEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
