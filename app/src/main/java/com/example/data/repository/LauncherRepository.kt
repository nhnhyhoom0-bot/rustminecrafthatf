package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import com.example.core.RustCoreBridge
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.ExternalLauncherTarget
import com.example.data.local.InstalledModEntity
import com.example.data.local.InstanceEntity
import com.example.data.local.LoaderType
import com.example.data.local.ModSource
import com.example.data.local.RustCraftDatabase
import com.example.data.local.SkinModel
import com.example.data.local.TouchLayoutEntity
import com.example.data.model.ExternalLauncherStatus
import com.example.data.model.ModSearchItem
import com.example.data.model.ModVersionOption
import com.example.data.model.TouchButtonConfig
import com.example.data.model.TouchControlDefaults
import com.example.data.remote.DeviceCodeResponse
import com.example.data.remote.MinecraftAuthResult
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class ScannedVersionResult(
    val foundCount: Int,
    val summaryMessage: String,
)

class LauncherRepository(
    private val context: Context,
    private val db: RustCraftDatabase,
) {
    val accounts: Flow<List<AccountEntity>> = db.accountDao().getAllAccounts()
    val activeAccount: Flow<AccountEntity?> = db.accountDao().getActiveAccount()
    val instances: Flow<List<InstanceEntity>> = db.instanceDao().getAllInstances()
    val selectedInstance: Flow<InstanceEntity?> = db.instanceDao().getSelectedInstance()
    val allInstalledMods: Flow<List<InstalledModEntity>> = db.installedModDao().getAllInstalledMods()
    val touchLayouts: Flow<List<TouchLayoutEntity>> = db.touchLayoutDao().getAllLayouts()
    val activeTouchLayout: Flow<TouchLayoutEntity?> = db.touchLayoutDao().getActiveLayout()

    // --- Modrinth & CurseForge Search + Install ---

    suspend fun searchModrinthMods(
        query: String,
        loader: String?,
        mcVersion: String?,
    ): Result<List<ModSearchItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val facetsList = mutableListOf("""["project_type:mod"]""")
            if (!loader.isNullOrBlank() && loader != "ALL" && loader != "VANILLA") {
                facetsList.add("""["categories:${loader.lowercase()}"]""")
            }
            if (!mcVersion.isNullOrBlank() && mcVersion != "ALL") {
                facetsList.add("""["versions:$mcVersion"]""")
            }
            val facets = "[${facetsList.joinToString(",")}]"
            val resp = NetworkModule.modrinthApi.searchMods(
                query = query.trim(),
                facets = facets,
                limit = 25,
            )
            resp.hits.map { hit ->
                ModSearchItem(
                    projectId = hit.projectId,
                    source = ModSource.MODRINTH.name,
                    title = hit.title,
                    description = hit.description,
                    author = hit.author,
                    downloads = hit.downloads,
                    iconUrl = hit.iconUrl,
                    categories = hit.categories,
                    gameVersions = hit.versions,
                    loaders = hit.categories.filter {
                        it.lowercase() in listOf("fabric", "forge", "neoforge", "quilt")
                    },
                )
            }
        }
    }

    suspend fun fetchModrinthVersions(projectId: String): Result<List<ModVersionOption>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkModule.modrinthApi.getProjectVersions(projectId).take(15).map { v ->
                    val file = v.files.firstOrNull { it.primary } ?: v.files.firstOrNull()
                    ModVersionOption(
                        versionId = v.id,
                        versionNumber = v.versionNumber,
                        fileName = file?.filename ?: "${v.versionNumber}.jar",
                        downloadUrl = file?.url.orEmpty(),
                        loaders = v.loaders,
                        gameVersions = v.gameVersions,
                    )
                }
            }
        }

    suspend fun searchCurseForgeMods(
        query: String,
        loader: String?,
        mcVersion: String?,
    ): Result<List<ModSearchItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = NetworkModule.getCurseForgeApiKey()
            if (key.isEmpty()) {
                throw IllegalStateException("مفتاح CurseForge غير مهيأ في أسرار التطبيق")
            }
            val loaderId = when (loader?.uppercase()) {
                "FORGE" -> 1
                "FABRIC" -> 4
                "QUILT" -> 5
                "NEOFORGE" -> 6
                else -> null
            }
            val resp = NetworkModule.curseForgeApi.searchMods(
                apiKey = key,
                searchFilter = query.trim(),
                gameVersion = mcVersion?.takeIf { it != "ALL" && it.isNotBlank() },
                modLoaderType = loaderId,
            )
            resp.data.map { m ->
                ModSearchItem(
                    projectId = m.id.toString(),
                    source = ModSource.CURSEFORGE.name,
                    title = m.name,
                    description = m.summary,
                    author = m.authors.firstOrNull()?.name ?: "CurseForge Author",
                    downloads = m.downloadCount,
                    iconUrl = m.logo?.thumbnailUrl ?: m.logo?.url,
                    categories = emptyList(),
                    gameVersions = m.latestFilesIndexes.map { it.gameVersion }.distinct(),
                    loaders = emptyList(),
                )
            }
        }
    }

    suspend fun fetchCurseForgeVersions(modId: Long): Result<List<ModVersionOption>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val key = NetworkModule.getCurseForgeApiKey()
                if (key.isEmpty()) throw IllegalStateException("مفتاح CurseForge مطلوب")
                NetworkModule.curseForgeApi.getModFiles(key, modId).data.map { f ->
                    ModVersionOption(
                        versionId = f.id.toString(),
                        versionNumber = f.displayName,
                        fileName = f.fileName,
                        downloadUrl = f.downloadUrl.orEmpty(),
                        loaders = emptyList(),
                        gameVersions = f.gameVersions,
                    )
                }
            }
        }

    suspend fun installModToInstance(
        instanceId: Long,
        mod: ModSearchItem,
        version: ModVersionOption,
        loader: String,
        mcVersion: String,
    ) {
        db.installedModDao().insertMod(
            InstalledModEntity(
                instanceId = instanceId,
                projectId = mod.projectId,
                source = mod.source,
                title = mod.title,
                author = mod.author,
                versionNumber = version.versionNumber,
                iconUrl = mod.iconUrl.orEmpty(),
                fileName = version.fileName,
                downloadUrl = version.downloadUrl,
                isEnabled = true,
                mcVersion = version.gameVersions.firstOrNull() ?: mcVersion,
                loaderType = version.loaders.firstOrNull()?.uppercase() ?: loader,
            )
        )
    }

    suspend fun importLocalModFile(instanceId: Long, uri: Uri, mcVersion: String, loader: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = resolveDisplayName(uri) ?: "imported_mod_${System.currentTimeMillis()}.jar"
                db.installedModDao().insertMod(
                    InstalledModEntity(
                        instanceId = instanceId,
                        projectId = "local_${System.currentTimeMillis()}",
                        source = ModSource.LOCAL.name,
                        title = name.substringBeforeLast("."),
                        author = "ملف محلي",
                        versionNumber = "Local",
                        fileName = name,
                        isEnabled = true,
                        mcVersion = mcVersion,
                        loaderType = loader,
                    )
                )
                name
            }
        }

    suspend fun toggleModEnabled(modId: Long, enabled: Boolean) {
        db.installedModDao().setModEnabled(modId, enabled)
    }

    suspend fun removeInstalledMod(modId: Long) {
        db.installedModDao().deleteModById(modId)
    }

    // --- Accounts & Skins ---

    suspend fun addOfflineAccount(username: String, skinServerUrl: String) {
        val cleanName = username.trim().ifEmpty { "Player" }
        val uuid = RustCoreBridge.generateOfflineUuid(cleanName)
        db.accountDao().deactivateAllAccounts()
        db.accountDao().insertAccount(
            AccountEntity(
                username = cleanName,
                uuid = uuid,
                accountType = AccountType.OFFLINE.name,
                skinModel = SkinModel.CLASSIC.name,
                capeStyle = "EMERALD_CAPE",
                skinServerUrl = skinServerUrl.trim(),
                isActive = true,
            )
        )
    }

    suspend fun activateAccount(id: Long) {
        db.accountDao().deactivateAllAccounts()
        db.accountDao().activateAccount(id)
    }

    suspend fun deleteAccount(id: Long) {
        db.accountDao().deleteAccountById(id)
    }

    suspend fun updateAccountSkinConfig(
        account: AccountEntity,
        skinUri: String = account.skinUri,
        skinModel: String = account.skinModel,
        capeStyle: String = account.capeStyle,
        skinServerUrl: String = account.skinServerUrl,
    ) {
        db.accountDao().updateAccount(
            account.copy(
                skinUri = skinUri,
                skinModel = skinModel,
                capeStyle = capeStyle,
                skinServerUrl = skinServerUrl,
            )
        )
    }

    suspend fun importSkinFromPhotoPicker(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("تعذر فتح ملف السكن")
            val bitmap = input.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalStateException("الملف المختار ليس صورة PNG صالحة")
            if (!RustCoreBridge.validateSkinDimensions(bitmap.width, bitmap.height)) {
                throw IllegalStateException(
                    "أبعاد السكن (${bitmap.width}×${bitmap.height}) غير صالحة. يجب أن تكون 64×64 أو 64×32",
                )
            }
            val dir = File(context.filesDir, "skins").apply { mkdirs() }
            val outFile = File(dir, "skin_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            outFile.absolutePath
        }
    }

    suspend fun fetchOnlineSkinByUsername(username: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = username.trim()
            if (clean.isEmpty()) throw IllegalArgumentException("أدخل اسم اللاعب أولاً")
            val req = okhttp3.Request.Builder()
                .url("https://minotar.net/skin/$clean")
                .get()
                .build()
            NetworkModule.okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("لم يتم العثور على سكن للاعب $clean")
                val bytes = resp.body?.bytes() ?: throw IllegalStateException("بيانات فارغة")
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("صورة غير صالحة")
                val dir = File(context.filesDir, "skins").apply { mkdirs() }
                val outFile = File(dir, "skin_${clean}_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { it.write(bytes) }
                bmp.recycle()
                outFile.absolutePath
            }
        }
    }

    suspend fun syncSkinToServer(account: AccountEntity): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (account.accountType == AccountType.MICROSOFT.name && account.accessToken.isNotBlank()) {
                val skinFile = File(account.skinUri)
                if (!skinFile.exists()) {
                    throw IllegalStateException("اختر ملف سكن PNG أولاً قبل الرفع إلى خوادم Mojang")
                }
                val bytes = skinFile.readBytes()
                val variant = if (account.skinModel == SkinModel.SLIM.name) "slim" else "classic"
                val ok = NetworkModule.microsoftAuthClient.uploadOfficialSkin(
                    mcToken = account.accessToken,
                    variant = variant,
                    pngBytes = bytes,
                )
                if (!ok) throw IllegalStateException("رفض خادم Mojang رفع السكن (تحقق من صلاحية الجلسة)")
                "تم رفع السكن رسمياً إلى حساب مايكروسوفت وسيظهر في جميع السيرفرات!"
            } else {
                val srv = account.skinServerUrl.trim()
                if (srv.isEmpty()) {
                    throw IllegalStateException("أدخل رابط خادم السكنات (مثل Ely.by أو LittleSkin)")
                }
                "تم ربط السكن بخادم ($srv) عبر Authlib-Injector للظهور في السيرفرات الداعمة!"
            }
        }
    }

    fun getMicrosoftClientId(): String = NetworkModule.microsoftAuthClient.getConfiguredClientId()

    suspend fun startMicrosoftDeviceCode(clientId: String): DeviceCodeResponse =
        NetworkModule.microsoftAuthClient.startDeviceCodeFlow(clientId)

    suspend fun completeMicrosoftDeviceCode(
        clientId: String,
        deviceCode: DeviceCodeResponse,
    ): MinecraftAuthResult {
        val result = NetworkModule.microsoftAuthClient.pollAndCompleteMinecraftAuth(clientId, deviceCode)
        db.accountDao().deactivateAllAccounts()
        db.accountDao().insertAccount(
            AccountEntity(
                username = result.username,
                uuid = result.uuid,
                accountType = AccountType.MICROSOFT.name,
                accessToken = result.mcAccessToken,
                refreshToken = result.msRefreshToken,
                skinModel = SkinModel.CLASSIC.name,
                capeStyle = "EMERALD_CAPE",
                skinServerUrl = "https://api.minecraftservices.com",
                isActive = true,
            )
        )
        return result
    }

    // --- External Ready-Version Scanner ONLY (Zalith / Mojo / Pojav / .minecraft) ---

    suspend fun selectInstance(id: Long) {
        db.instanceDao().clearSelectedInstance()
        db.instanceDao().selectInstance(id)
    }

    suspend fun deleteInstance(id: Long) {
        db.instanceDao().deleteInstanceById(id)
    }

    suspend fun scanAllReadyExternalVersions(savedTreeUris: Set<String>): ScannedVersionResult =
        withContext(Dispatchers.IO) {
            val discovered = mutableListOf<InstanceEntity>()

            // 1. Scan accessible local launcher directories on shared storage (if readable)
            val extRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
            if (extRoot != null) {
                val candidateDirs = listOf(
                    File(extRoot, "games/ZalithLauncher/.minecraft/versions") to ExternalLauncherTarget.ZALITH,
                    File(extRoot, "ZalithLauncher/.minecraft/versions") to ExternalLauncherTarget.ZALITH,
                    File(extRoot, "games/MojoLauncher/.minecraft/versions") to ExternalLauncherTarget.MOJO,
                    File(extRoot, "MojoLauncher/.minecraft/versions") to ExternalLauncherTarget.MOJO,
                    File(extRoot, "games/PojavLauncher/.minecraft/versions") to ExternalLauncherTarget.POJAV,
                    File(extRoot, ".minecraft/versions") to ExternalLauncherTarget.ZALITH,
                )
                for ((versionsDir, launcherTarget) in candidateDirs) {
                    if (versionsDir.exists() && versionsDir.isDirectory) {
                        versionsDir.listFiles()?.filter { it.isDirectory }?.forEach { verDir ->
                            val jsonFile = File(verDir, "${verDir.name}.json")
                            if (jsonFile.exists() && jsonFile.isFile) {
                                runCatching { jsonFile.readText() }.getOrNull()?.let { text ->
                                    parseVersionJsonToInstance(
                                        jsonText = text,
                                        folderName = verDir.name,
                                        externalUri = jsonFile.absolutePath,
                                        launcherTarget = launcherTarget,
                                    )?.let { discovered.add(it) }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Scan user-authorized SAF folders (Zalith / Mojo / Pojav / .minecraft)
            savedTreeUris.forEach { uriStr ->
                runCatching {
                    val treeUri = Uri.parse(uriStr)
                    discovered.addAll(scanSafTreeForVersionJsons(treeUri))
                }
            }

            upsertDiscoveredInstances(discovered)

            val installedLaunchers = detectExternalLaunchers().filter { it.isInstalled }
            val summary = when {
                discovered.isNotEmpty() ->
                    "تم العثور على ${discovered.size} نسخة جاهزة مثبتة من لانشر خارجي!"
                installedLaunchers.isNotEmpty() ->
                    "تم رصد ${installedLaunchers.joinToString(" و ") { it.name }} على الجهاز. اختر مجلد .minecraft أو versions للوصول إلى النسخ المثبتة."
                else ->
                    "لم يتم العثور على نسخ جاهزة. حدد مجلد لانشر آخر (مثل Zalith أو Mojo) يحتوي على مجلد versions."
            }
            ScannedVersionResult(foundCount = discovered.size, summaryMessage = summary)
        }

    suspend fun scanSafFolderAndSave(treeUri: Uri): Result<ScannedVersionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val discovered = scanSafTreeForVersionJsons(treeUri)
                if (discovered.isEmpty()) {
                    throw IllegalStateException(
                        "لم يتم العثور على ملفات نسخة جاهزة (<version>.json) داخل المجلد المحدد. تأكد من اختيار مجلد .minecraft أو versions من لانشر Zalith أو Mojo."
                    )
                }
                upsertDiscoveredInstances(discovered)
                ScannedVersionResult(
                    foundCount = discovered.size,
                    summaryMessage = "تم استيراد والعثور على ${discovered.size} نسخة جاهزة بنجاح!",
                )
            }
        }

    private suspend fun upsertDiscoveredInstances(discovered: List<InstanceEntity>) {
        if (discovered.isEmpty()) return
        val existing = db.instanceDao().getAllInstances().first()
        var hasSelected = existing.any { it.isSelected }

        discovered.forEach { item ->
            val match = existing.firstOrNull {
                it.externalPathUri == item.externalPathUri ||
                    (it.name == item.name && it.sourceLauncher == item.sourceLauncher)
            }
            if (match == null) {
                val selectNow = !hasSelected
                db.instanceDao().insertInstance(item.copy(isSelected = selectNow))
                if (selectNow) hasSelected = true
            } else {
                db.instanceDao().updateInstance(
                    match.copy(
                        mcVersion = item.mcVersion,
                        loaderType = item.loaderType,
                        loaderVersion = item.loaderVersion,
                        isReady = true,
                    )
                )
            }
        }
    }

    private fun scanSafTreeForVersionJsons(treeUri: Uri): List<InstanceEntity> {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val launcherTarget = detectTargetFromPath(rootDocId)
        val results = mutableListOf<InstanceEntity>()

        fun walk(docId: String, currentDirName: String, depth: Int) {
            if (depth > 3) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idCol)
                    val childName = cursor.getString(nameCol)
                    val mime = cursor.getString(mimeCol)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // Prioritize versions/ folders or recurse shallowly
                        if (childName.equals("versions", ignoreCase = true) ||
                            childName.equals(".minecraft", ignoreCase = true) ||
                            currentDirName.equals("versions", ignoreCase = true) ||
                            depth == 0
                        ) {
                            walk(childId, childName, depth + 1)
                        }
                    } else if (childName.equals("$currentDirName.json", ignoreCase = true)) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        val jsonText = runCatching {
                            context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                        }.getOrNull()
                        if (!jsonText.isNullOrBlank()) {
                            parseVersionJsonToInstance(
                                jsonText = jsonText,
                                folderName = currentDirName,
                                externalUri = fileUri.toString(),
                                launcherTarget = launcherTarget,
                            )?.let { results.add(it) }
                        }
                    }
                }
            }
        }

        val rootFolderName = rootDocId.substringAfterLast(":").substringAfterLast("/")
        walk(rootDocId, rootFolderName, 0)
        return results
    }

    private fun detectTargetFromPath(path: String): ExternalLauncherTarget {
        val lower = path.lowercase()
        return when {
            "zalith" in lower -> ExternalLauncherTarget.ZALITH
            "mojo" in lower -> ExternalLauncherTarget.MOJO
            "pojav" in lower -> ExternalLauncherTarget.POJAV
            else -> ExternalLauncherTarget.ZALITH
        }
    }

    private fun parseVersionJsonToInstance(
        jsonText: String,
        folderName: String,
        externalUri: String,
        launcherTarget: ExternalLauncherTarget,
    ): InstanceEntity? {
        return runCatching {
            val json = JSONObject(jsonText)
            val id = json.optString("id", folderName).ifBlank { folderName }
            val inheritsFrom = json.optString("inheritsFrom", "").trim()
            val librariesStr = json.optJSONArray("libraries")?.toString()?.lowercase().orEmpty()
            val combined = "$id $librariesStr".lowercase()

            val loader = when {
                "neoforge" in combined -> LoaderType.NEOFORGE
                "fabric" in combined -> LoaderType.FABRIC
                "quilt" in combined -> LoaderType.QUILT
                "forge" in combined -> LoaderType.FORGE
                else -> LoaderType.VANILLA
            }
            val mcVersion = inheritsFrom.ifBlank {
                Regex("""\d+\.\d+(\.\d+)?""").find(id)?.value ?: id
            }
            val loaderVersion = if (loader == LoaderType.VANILLA) {
                "Vanilla"
            } else {
                id.removePrefix(mcVersion).trim('-', '_', ' ').ifBlank { loader.displayName }
            }

            InstanceEntity(
                name = id,
                mcVersion = mcVersion,
                loaderType = loader.name,
                loaderVersion = loaderVersion,
                ramMb = 2560,
                sourceLauncher = launcherTarget.name,
                externalPathUri = externalUri,
                isReady = true,
                isSelected = false,
            )
        }.getOrNull()
    }

    fun detectExternalLaunchers(): List<ExternalLauncherStatus> {
        val pm = context.packageManager
        fun isInstalled(pkg: String): Boolean = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: Throwable) {
            false
        }
        return listOf(
            ExternalLauncherStatus(
                name = "Zalith Launcher",
                packageName = "com.movtery.zalithlauncher",
                isInstalled = isInstalled("com.movtery.zalithlauncher"),
                description = "لانشر خارجي لتنزيل وتشغيل نسخ ماين كرافت جافا والمودات على أندرويد",
                downloadUrl = "https://github.com/MovTery/ZalithLauncher/releases",
            ),
            ExternalLauncherStatus(
                name = "MojoLauncher",
                packageName = "git.artdeell.mojo",
                isInstalled = isInstalled("git.artdeell.mojo"),
                description = "لانشر سريع وخفيف لتنزيل نسخ ماين كرافت الجاهزة وتشغيلها",
                downloadUrl = "https://github.com/MojoLauncher/MojoLauncher/releases",
            ),
            ExternalLauncherStatus(
                name = "PojavLauncher",
                packageName = "net.kdt.pojavlaunch",
                isInstalled = isInstalled("net.kdt.pojavlaunch"),
                description = "اللانشر الكلاسيكي لتشغيل بيئة LWJGL وملفات ماين كرافت جافا",
                downloadUrl = "https://github.com/PojavLauncherTeam/PojavLauncher/releases",
            ),
        )
    }

    fun launchExternalLauncherApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    // --- Touch Control Layouts ---

    suspend fun activateTouchLayout(id: Long) {
        db.touchLayoutDao().clearActiveLayout()
        db.touchLayoutDao().activateLayout(id)
    }

    suspend fun saveTouchLayout(layout: TouchLayoutEntity, buttons: List<TouchButtonConfig>) {
        db.touchLayoutDao().updateLayout(
            layout.copy(
                buttonsJson = TouchControlDefaults.encodeButtons(buttons),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun createTouchLayout(name: String) {
        db.touchLayoutDao().clearActiveLayout()
        db.touchLayoutDao().insertLayout(
            TouchLayoutEntity(
                name = name.trim().ifEmpty { "تخطيط مخصص" },
                isActive = true,
                buttonsJson = TouchControlDefaults.encodeButtons(TouchControlDefaults.pvpPreset()),
            )
        )
    }

    suspend fun deleteTouchLayout(id: Long) {
        db.touchLayoutDao().deleteLayoutById(id)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
