package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.RustCoreBridge
import com.example.data.local.AccountEntity
import com.example.data.local.ExternalLauncherTarget
import com.example.data.local.InstalledModEntity
import com.example.data.local.InstanceEntity
import com.example.data.local.LauncherSettings
import com.example.data.local.RustCraftDatabase
import com.example.data.local.SettingsRepository
import com.example.data.local.TouchLayoutEntity
import com.example.data.model.ExternalLauncherStatus
import com.example.data.model.MicrosoftLoginState
import com.example.data.model.ModSearchItem
import com.example.data.model.ModVersionOption
import com.example.data.model.TouchButtonConfig
import com.example.data.model.TouchControlDefaults
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModStoreTab {
    MODRINTH,
    CURSEFORGE,
    INSTALLED,
}

data class LauncherUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val activeAccount: AccountEntity? = null,
    val instances: List<InstanceEntity> = emptyList(),
    val selectedInstance: InstanceEntity? = null,
    val installedMods: List<InstalledModEntity> = emptyList(),
    val touchLayouts: List<TouchLayoutEntity> = emptyList(),
    val activeTouchLayout: TouchLayoutEntity? = null,
    val settings: LauncherSettings = LauncherSettings(),
    val externalLaunchers: List<ExternalLauncherStatus> = emptyList(),
    // External Ready-Version Scanner
    val isScanningVersions: Boolean = false,
    val lastScanSummary: String? = null,
    // Mods Search
    val modStoreTab: ModStoreTab = ModStoreTab.MODRINTH,
    val modSearchQuery: String = "",
    val modLoaderFilter: String = "ALL",
    val modVersionFilter: String = "ALL",
    val modSearchResults: List<ModSearchItem> = emptyList(),
    val isSearchingMods: Boolean = false,
    val modSearchError: String? = null,
    val selectedModForInstall: ModSearchItem? = null,
    val modVersionOptions: List<ModVersionOption> = emptyList(),
    val isLoadingModVersions: Boolean = false,
    // Microsoft Auth
    val microsoftLoginState: MicrosoftLoginState = MicrosoftLoginState.Idle,
    // Play Sandbox Overlay
    val isPlayOverlayActive: Boolean = false,
    val launchLogs: List<String> = emptyList(),
    // Feedback message
    val snackbarMessage: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val db = RustCraftDatabase.getInstance(application)
    private val repository = LauncherRepository(application, db)
    private val settingsRepo = SettingsRepository(application)

    private val _transient = MutableStateFlow(TransientState())
    private var msAuthJob: Job? = null

    private data class TransientState(
        val externalLaunchers: List<ExternalLauncherStatus> = emptyList(),
        val isScanningVersions: Boolean = false,
        val lastScanSummary: String? = null,
        val modStoreTab: ModStoreTab = ModStoreTab.MODRINTH,
        val modSearchQuery: String = "",
        val modLoaderFilter: String = "ALL",
        val modVersionFilter: String = "ALL",
        val modSearchResults: List<ModSearchItem> = emptyList(),
        val isSearchingMods: Boolean = false,
        val modSearchError: String? = null,
        val selectedModForInstall: ModSearchItem? = null,
        val modVersionOptions: List<ModVersionOption> = emptyList(),
        val isLoadingModVersions: Boolean = false,
        val microsoftLoginState: MicrosoftLoginState = MicrosoftLoginState.Idle,
        val isPlayOverlayActive: Boolean = false,
        val launchLogs: List<String> = emptyList(),
        val snackbarMessage: String? = null,
    )

    private val coreFlows = combine(
        repository.accounts,
        repository.activeAccount,
        repository.instances,
        repository.selectedInstance,
        repository.allInstalledMods,
    ) { accounts, activeAcc, instances, selectedInst, allMods ->
        CoreDbSnapshot(accounts, activeAcc, instances, selectedInst, allMods)
    }

    private val secondaryFlows = combine(
        repository.touchLayouts,
        repository.activeTouchLayout,
        settingsRepo.settings,
        _transient,
    ) { layouts, activeLayout, settings, transient ->
        SecondarySnapshot(layouts, activeLayout, settings, transient)
    }

    val uiState: StateFlow<LauncherUiState> = combine(coreFlows, secondaryFlows) { core, sec ->
        val selectedInstId = core.selectedInstance?.id
        val filteredMods = if (selectedInstId != null) {
            core.allMods.filter { it.instanceId == selectedInstId }
        } else {
            core.allMods
        }
        LauncherUiState(
            accounts = core.accounts,
            activeAccount = core.activeAccount,
            instances = core.instances,
            selectedInstance = core.selectedInstance,
            installedMods = filteredMods,
            touchLayouts = sec.layouts,
            activeTouchLayout = sec.activeLayout,
            settings = sec.settings,
            externalLaunchers = sec.transient.externalLaunchers,
            isScanningVersions = sec.transient.isScanningVersions,
            lastScanSummary = sec.transient.lastScanSummary,
            modStoreTab = sec.transient.modStoreTab,
            modSearchQuery = sec.transient.modSearchQuery,
            modLoaderFilter = sec.transient.modLoaderFilter,
            modVersionFilter = sec.transient.modVersionFilter,
            modSearchResults = sec.transient.modSearchResults,
            isSearchingMods = sec.transient.isSearchingMods,
            modSearchError = sec.transient.modSearchError,
            selectedModForInstall = sec.transient.selectedModForInstall,
            modVersionOptions = sec.transient.modVersionOptions,
            isLoadingModVersions = sec.transient.isLoadingModVersions,
            microsoftLoginState = sec.transient.microsoftLoginState,
            isPlayOverlayActive = sec.transient.isPlayOverlayActive,
            launchLogs = sec.transient.launchLogs,
            snackbarMessage = sec.transient.snackbarMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState(),
    )

    private data class CoreDbSnapshot(
        val accounts: List<AccountEntity>,
        val activeAccount: AccountEntity?,
        val instances: List<InstanceEntity>,
        val selectedInstance: InstanceEntity?,
        val allMods: List<InstalledModEntity>,
    )

    private data class SecondarySnapshot(
        val layouts: List<TouchLayoutEntity>,
        val activeLayout: TouchLayoutEntity?,
        val settings: LauncherSettings,
        val transient: TransientState,
    )

    init {
        refreshExternalLaunchers()
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            if (s.autoScanOnStartup) {
                scanForReadyVersions(silentIfEmpty = true)
            }
            searchMods()
        }
    }

    fun clearSnackbar() {
        _transient.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        _transient.update { it.copy(snackbarMessage = msg) }
    }

    fun refreshExternalLaunchers() {
        _transient.update {
            it.copy(externalLaunchers = repository.detectExternalLaunchers())
        }
    }

    // --- External Ready-Version Scanner ---

    fun scanForReadyVersions(silentIfEmpty: Boolean = false) {
        viewModelScope.launch {
            _transient.update { it.copy(isScanningVersions = true) }
            refreshExternalLaunchers()
            val savedUris = settingsRepo.settings.first().savedScannerTreeUris
            val result = repository.scanAllReadyExternalVersions(savedUris)
            _transient.update {
                it.copy(
                    isScanningVersions = false,
                    lastScanSummary = result.summaryMessage,
                    snackbarMessage = if (silentIfEmpty && result.foundCount == 0) {
                        null
                    } else {
                        result.summaryMessage
                    },
                )
            }
        }
    }

    fun importExternalInstance(uri: Uri) {
        viewModelScope.launch {
            _transient.update { it.copy(isScanningVersions = true) }
            repository.scanSafFolderAndSave(uri)
                .onSuccess { res ->
                    settingsRepo.addScannerTreeUri(uri.toString())
                    _transient.update {
                        it.copy(
                            isScanningVersions = false,
                            lastScanSummary = res.summaryMessage,
                            snackbarMessage = res.summaryMessage,
                        )
                    }
                }
                .onFailure { err ->
                    _transient.update {
                        it.copy(
                            isScanningVersions = false,
                            lastScanSummary = err.localizedMessage,
                            snackbarMessage = err.localizedMessage ?: "لم يتم العثور على نسخ جاهزة في المجلد",
                        )
                    }
                }
        }
    }

    fun selectInstance(id: Long) {
        viewModelScope.launch { repository.selectInstance(id) }
    }

    fun deleteInstance(id: Long) {
        viewModelScope.launch { repository.deleteInstance(id) }
    }

    fun launchSelectedInstance() {
        val state = uiState.value
        val inst = state.selectedInstance
        val account = state.activeAccount
        if (inst == null) {
            showMessage("ابحث أولاً عن نسخة جاهزة مثبتة من لانشر آخر (مثل Zalith أو Mojo)")
            return
        }
        val target = runCatching { ExternalLauncherTarget.valueOf(inst.sourceLauncher) }
            .getOrDefault(ExternalLauncherTarget.ZALITH)

        if (target.packageName != null) {
            val launched = repository.launchExternalLauncherApp(target.packageName)
            if (launched) {
                showMessage("تم تمرير التشغيل للنسخة الجاهزة عبر ${target.displayName}")
                return
            }
        }

        // Launch RustCraft Touch HUD Overlay when the external launcher app isn't directly callable
        val user = account?.username ?: "RustBuilder"
        val uuid = account?.uuid ?: RustCoreBridge.generateOfflineUuid(user)
        val args = RustCoreBridge.buildJvmLaunchArgs(
            ramMb = inst.ramMb,
            username = user,
            uuid = uuid,
            mcVersion = inst.mcVersion,
            loader = inst.loaderType,
            skinServerUrl = account?.skinServerUrl,
            renderer = state.settings.renderer,
        )
        val enabledMods = state.installedMods.filter { it.isEnabled }
        val logs = buildList {
            add("[RUST-CORE] ${RustCoreBridge.getEngineBanner()}")
            add("[RUST-CORE] Mode: ${RustCoreBridge.engineModeLabel}")
            add("[READY-VERSION] Detected External Instance: ${inst.name}")
            add("[SOURCE] Launcher=${target.displayName} | Path=${inst.externalPathUri}")
            add("[AUTH] Player=$user | Type=${account?.accountType ?: "OFFLINE"} | UUID=$uuid")
            add("[SKIN] Model=${account?.skinModel ?: "CLASSIC"} | Server=${account?.skinServerUrl ?: "Default"}")
            add("[MODS] Active mods (${enabledMods.size}): ${enabledMods.joinToString { it.title }.ifEmpty { "Base Instance" }}")
            add("[JVM] ${args.joinToString(" ")}")
            add("[TOUCH] Active HUD: ${state.activeTouchLayout?.name ?: "الافتراضي"}")
            add("[RENDER] ${state.settings.renderer} @ ${(state.settings.resolutionScale * 100).toInt()}%")
        }
        _transient.update {
            it.copy(isPlayOverlayActive = true, launchLogs = logs)
        }
    }

    fun closePlayOverlay() {
        _transient.update { it.copy(isPlayOverlayActive = false) }
    }

    fun openExternalLauncher(pkg: String, fallbackName: String) {
        if (!repository.launchExternalLauncherApp(pkg)) {
            showMessage("$fallbackName غير مثبت على الجهاز حالياً")
        }
    }

    // --- Mod Store (Modrinth & CurseForge) ---

    fun setModStoreTab(tab: ModStoreTab) {
        _transient.update { it.copy(modStoreTab = tab, modSearchError = null) }
        if (tab != ModStoreTab.INSTALLED) {
            searchMods()
        }
    }

    fun updateModSearchQuery(query: String) {
        _transient.update { it.copy(modSearchQuery = query) }
    }

    fun updateModFilters(loader: String? = null, version: String? = null) {
        _transient.update {
            it.copy(
                modLoaderFilter = loader ?: it.modLoaderFilter,
                modVersionFilter = version ?: it.modVersionFilter,
            )
        }
        searchMods()
    }

    fun searchMods() {
        viewModelScope.launch {
            val state = _transient.value
            if (state.modStoreTab == ModStoreTab.INSTALLED) return@launch
            _transient.update { it.copy(isSearchingMods = true, modSearchError = null) }

            val result = when (state.modStoreTab) {
                ModStoreTab.MODRINTH -> repository.searchModrinthMods(
                    query = state.modSearchQuery,
                    loader = state.modLoaderFilter,
                    mcVersion = state.modVersionFilter,
                )
                ModStoreTab.CURSEFORGE -> repository.searchCurseForgeMods(
                    query = state.modSearchQuery,
                    loader = state.modLoaderFilter,
                    mcVersion = state.modVersionFilter,
                )
                ModStoreTab.INSTALLED -> Result.success(emptyList())
            }

            result.onSuccess { list ->
                _transient.update {
                    it.copy(isSearchingMods = false, modSearchResults = list, modSearchError = null)
                }
            }.onFailure { err ->
                _transient.update {
                    it.copy(
                        isSearchingMods = false,
                        modSearchResults = emptyList(),
                        modSearchError = err.localizedMessage ?: "خطأ في البحث",
                    )
                }
            }
        }
    }

    fun openModVersionPicker(mod: ModSearchItem) {
        _transient.update {
            it.copy(
                selectedModForInstall = mod,
                modVersionOptions = emptyList(),
                isLoadingModVersions = true,
            )
        }
        viewModelScope.launch {
            val res = if (mod.source == "CURSEFORGE") {
                repository.fetchCurseForgeVersions(mod.projectId.toLongOrNull() ?: 0L)
            } else {
                repository.fetchModrinthVersions(mod.projectId)
            }
            res.onSuccess { versions ->
                _transient.update {
                    it.copy(isLoadingModVersions = false, modVersionOptions = versions)
                }
            }.onFailure { err ->
                _transient.update {
                    it.copy(
                        isLoadingModVersions = false,
                        snackbarMessage = "تعذر جلب إصدارات المود: ${err.localizedMessage}",
                    )
                }
            }
        }
    }

    fun closeModVersionPicker() {
        _transient.update { it.copy(selectedModForInstall = null, modVersionOptions = emptyList()) }
    }

    fun installModVersion(option: ModVersionOption) {
        viewModelScope.launch {
            val mod = _transient.value.selectedModForInstall ?: return@launch
            val inst = uiState.value.selectedInstance ?: uiState.value.instances.firstOrNull()
            if (inst == null) {
                _transient.update { it.copy(selectedModForInstall = null) }
                showMessage("ابحث عن نسخة جاهزة مثبتة من لانشر آخر أولاً لتركيب المود داخلها")
                return@launch
            }
            repository.installModToInstance(
                instanceId = inst.id,
                mod = mod,
                version = option,
                loader = inst.loaderType,
                mcVersion = inst.mcVersion,
            )
            _transient.update {
                it.copy(
                    selectedModForInstall = null,
                    modVersionOptions = emptyList(),
                    snackbarMessage = "تم تركيب '${mod.title}' (${option.versionNumber}) داخل '${inst.name}'",
                )
            }
        }
    }

    fun importLocalMod(uri: Uri) {
        viewModelScope.launch {
            val inst = uiState.value.selectedInstance ?: uiState.value.instances.firstOrNull()
            if (inst == null) {
                showMessage("ابحث عن نسخة جاهزة أولاً قبل استيراد المودات")
                return@launch
            }
            repository.importLocalModFile(inst.id, uri, inst.mcVersion, inst.loaderType)
                .onSuccess { name -> showMessage("تم استيراد المود '$name' إلى '${inst.name}'") }
                .onFailure { err -> showMessage("فشل استيراد الملف: ${err.localizedMessage}") }
        }
    }

    fun toggleMod(modId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.toggleModEnabled(modId, enabled) }
    }

    fun deleteMod(modId: Long) {
        viewModelScope.launch {
            repository.removeInstalledMod(modId)
            showMessage("تمت إزالة المود")
        }
    }

    // --- Accounts & Skins ---

    fun addOfflineAccount(username: String, skinServerUrl: String) {
        viewModelScope.launch {
            repository.addOfflineAccount(username, skinServerUrl)
            showMessage("تمت إضافة حساب كراك: $username")
        }
    }

    fun activateAccount(id: Long) {
        viewModelScope.launch { repository.activateAccount(id) }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch { repository.deleteAccount(id) }
    }

    fun importSkinUri(uri: Uri) {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.importSkinFromPhotoPicker(uri)
                .onSuccess { path ->
                    repository.updateAccountSkinConfig(active, skinUri = path)
                    showMessage("تم استيراد السكن بنجاح وتجهيزه للظهور في السيرفرات")
                }
                .onFailure { err -> showMessage(err.localizedMessage ?: "صورة سكن غير صالحة") }
        }
    }

    fun fetchSkinFromUsername(username: String) {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.fetchOnlineSkinByUsername(username)
                .onSuccess { path ->
                    repository.updateAccountSkinConfig(active, skinUri = path)
                    showMessage("تم تحميل سكن اللاعب '$username' بنجاح")
                }
                .onFailure { err -> showMessage(err.localizedMessage ?: "تعذر جلب السكن") }
        }
    }

    fun updateSkinModel(modelName: String) {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.updateAccountSkinConfig(active, skinModel = modelName)
        }
    }

    fun updateCapeStyle(capeId: String) {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.updateAccountSkinConfig(active, capeStyle = capeId)
        }
    }

    fun updateSkinServerUrl(url: String) {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.updateAccountSkinConfig(active, skinServerUrl = url)
            showMessage("تم حفظ خادم السكنات للسيرفرات")
        }
    }

    fun syncSkinToMultiplayerServers() {
        viewModelScope.launch {
            val active = uiState.value.activeAccount ?: return@launch
            repository.syncSkinToServer(active)
                .onSuccess { msg -> showMessage(msg) }
                .onFailure { err -> showMessage(err.localizedMessage ?: "فشلت مزامنة السكن") }
        }
    }

    fun startMicrosoftLogin() {
        val clientId = repository.getMicrosoftClientId()
        if (clientId.isEmpty()) {
            _transient.update {
                it.copy(
                    microsoftLoginState = MicrosoftLoginState.Error(
                        "قم بإضافة MICROSOFT_CLIENT_ID في لوحة Secrets في AI Studio لتفعيل تسجيل دخول مايكروسوفت الرسمي (Device Code OAuth2)."
                    )
                )
            }
            return
        }
        msAuthJob?.cancel()
        msAuthJob = viewModelScope.launch {
            _transient.update { it.copy(microsoftLoginState = MicrosoftLoginState.RequestingCode) }
            runCatching {
                val dc = repository.startMicrosoftDeviceCode(clientId)
                _transient.update {
                    it.copy(
                        microsoftLoginState = MicrosoftLoginState.WaitingForUser(
                            userCode = dc.userCode,
                            verificationUri = dc.verificationUri,
                            expiresInSeconds = dc.expiresIn,
                            statusText = "افتح الرابط وأدخل الرمز ثم وافق على الصلاحيات...",
                        )
                    )
                }
                repository.completeMicrosoftDeviceCode(clientId, dc)
            }.onSuccess { result ->
                _transient.update {
                    it.copy(
                        microsoftLoginState = MicrosoftLoginState.Success(result.username, result.uuid),
                        snackbarMessage = "مرحباً ${result.username}! تم ربط حساب مايكروسوفت الأصلي.",
                    )
                }
            }.onFailure { err ->
                _transient.update {
                    it.copy(
                        microsoftLoginState = MicrosoftLoginState.Error(
                            err.localizedMessage ?: "فشل تسجيل الدخول بحساب مايكروسوفت",
                        )
                    )
                }
            }
        }
    }

    fun resetMicrosoftLoginState() {
        msAuthJob?.cancel()
        _transient.update { it.copy(microsoftLoginState = MicrosoftLoginState.Idle) }
    }

    // --- Touch Layout Editor ---

    fun activateTouchLayout(id: Long) {
        viewModelScope.launch { repository.activateTouchLayout(id) }
    }

    fun createTouchLayout(name: String) {
        viewModelScope.launch {
            repository.createTouchLayout(name)
            showMessage("تم إنشاء تخطيط لمس جديد: $name")
        }
    }

    fun saveTouchButtons(layout: TouchLayoutEntity, buttons: List<TouchButtonConfig>) {
        viewModelScope.launch {
            repository.saveTouchLayout(layout, buttons)
            showMessage("تم حفظ تخطيط أزرار اللمس")
        }
    }

    fun updateTouchLayoutPreferences(
        layout: TouchLayoutEntity,
        opacity: Float = layout.globalOpacity,
        haptic: Boolean = layout.hapticEnabled,
    ) {
        viewModelScope.launch {
            repository.saveTouchLayout(
                layout.copy(globalOpacity = opacity, hapticEnabled = haptic),
                TouchControlDefaults.decodeButtons(layout.buttonsJson),
            )
        }
    }

    // --- Settings ---

    fun updateRam(mb: Int) = viewModelScope.launch { settingsRepo.updateRam(mb) }
    fun updateJavaRuntime(runtime: String) = viewModelScope.launch { settingsRepo.updateJavaRuntime(runtime) }
    fun updateRenderer(renderer: String) = viewModelScope.launch { settingsRepo.updateRenderer(renderer) }
    fun updateResolutionScale(scale: Float) = viewModelScope.launch { settingsRepo.updateResolutionScale(scale) }
    fun updateAutoScanOnStartup(enabled: Boolean) =
        viewModelScope.launch { settingsRepo.updateAutoScanOnStartup(enabled) }
    fun clearSavedScannerFolders() {
        viewModelScope.launch {
            settingsRepo.clearScannerTreeUris()
            showMessage("تم مسح مسارات المسح المحفوظة")
        }
    }
}
