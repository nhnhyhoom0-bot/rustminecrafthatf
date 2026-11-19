package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Mojang Manifest DTOs & Service ---

@JsonClass(generateAdapter = true)
data class MojangManifestResponse(
    val latest: MojangLatest,
    val versions: List<MojangVersionDto>,
)

@JsonClass(generateAdapter = true)
data class MojangLatest(
    val release: String,
    val snapshot: String,
)

@JsonClass(generateAdapter = true)
data class MojangVersionDto(
    val id: String,
    val type: String,
    val url: String,
    val releaseTime: String,
)

interface MojangApiService {
    @GET("mc/game/version_manifest_v2.json")
    suspend fun getVersionManifest(): MojangManifestResponse
}

// --- Fabric Meta DTOs & Service ---

@JsonClass(generateAdapter = true)
data class FabricLoaderDto(
    val separator: String? = null,
    val build: Int? = null,
    val version: String,
    val stable: Boolean = false,
)

interface FabricMetaService {
    @GET("v2/versions/loader")
    suspend fun getLoaderVersions(): List<FabricLoaderDto>
}

// --- Modrinth API v2 DTOs & Service ---

@JsonClass(generateAdapter = true)
data class ModrinthSearchResponse(
    val hits: List<ModrinthHitDto>,
    @Json(name = "total_hits") val totalHits: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ModrinthHitDto(
    @Json(name = "project_id") val projectId: String,
    val title: String,
    val description: String = "",
    val author: String = "",
    val downloads: Long = 0L,
    @Json(name = "icon_url") val iconUrl: String? = null,
    val categories: List<String> = emptyList(),
    val versions: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ModrinthVersionDto(
    val id: String,
    val name: String = "",
    @Json(name = "version_number") val versionNumber: String,
    @Json(name = "game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val files: List<ModrinthFileDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ModrinthFileDto(
    val url: String,
    val filename: String,
    val primary: Boolean = false,
)

interface ModrinthApiService {
    @GET("search")
    suspend fun searchMods(
        @Query("query") query: String,
        @Query("facets") facets: String,
        @Query("limit") limit: Int = 25,
        @Query("index") index: String = "downloads",
    ): ModrinthSearchResponse

    @GET("project/{id}/version")
    suspend fun getProjectVersions(
        @Path("id") projectId: String,
    ): List<ModrinthVersionDto>
}

// --- CurseForge API v1 DTOs & Service ---

@JsonClass(generateAdapter = true)
data class CurseForgeSearchResponse(
    val data: List<CurseForgeModDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CurseForgeModDto(
    val id: Long,
    val name: String,
    val summary: String = "",
    val downloadCount: Long = 0L,
    val logo: CurseForgeLogoDto? = null,
    val authors: List<CurseForgeAuthorDto> = emptyList(),
    val latestFilesIndexes: List<CurseForgeFileIndexDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CurseForgeLogoDto(
    val thumbnailUrl: String? = null,
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class CurseForgeAuthorDto(
    val name: String,
)

@JsonClass(generateAdapter = true)
data class CurseForgeFileIndexDto(
    val gameVersion: String = "",
    val fileId: Long = 0L,
    val filename: String = "",
    val modLoader: Int? = null,
)

@JsonClass(generateAdapter = true)
data class CurseForgeFilesResponse(
    val data: List<CurseForgeFileDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CurseForgeFileDto(
    val id: Long,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String? = null,
    val gameVersions: List<String> = emptyList(),
)

interface CurseForgeApiService {
    @GET("mods/search")
    suspend fun searchMods(
        @Header("x-api-key") apiKey: String,
        @Query("gameId") gameId: Int = 432, // Minecraft
        @Query("classId") classId: Int = 6,  // Mods
        @Query("searchFilter") searchFilter: String,
        @Query("gameVersion") gameVersion: String? = null,
        @Query("modLoaderType") modLoaderType: Int? = null,
        @Query("sortField") sortField: Int = 2, // Popularity
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("pageSize") pageSize: Int = 25,
    ): CurseForgeSearchResponse

    @GET("mods/{modId}/files")
    suspend fun getModFiles(
        @Header("x-api-key") apiKey: String,
        @Path("modId") modId: Long,
        @Query("pageSize") pageSize: Int = 15,
    ): CurseForgeFilesResponse
}

// --- Microsoft MSA Device Code + XBL + XSTS + Minecraft Services ---

data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
)

data class MinecraftAuthResult(
    val username: String,
    val uuid: String,
    val mcAccessToken: String,
    val msRefreshToken: String,
)

class MicrosoftAuthClient(private val httpClient: OkHttpClient) {

    companion object {
        private const val SCOPE = "XboxLive.signin offline_access"
        private const val DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        private const val TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        private const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val MC_LOGIN_URL =
            "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val MC_PROFILE_URL =
            "https://api.minecraftservices.com/minecraft/profile"
        private const val MC_SKINS_URL =
            "https://api.minecraftservices.com/minecraft/profile/skins"
    }

    fun getConfiguredClientId(): String {
        val raw = BuildConfig.MICROSOFT_CLIENT_ID.trim()
        return if (raw.isEmpty() || raw == "YOUR_AZURE_APP_CLIENT_ID") "" else raw
    }

    suspend fun startDeviceCodeFlow(clientId: String): DeviceCodeResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("scope", SCOPE)
                .build()
            val req = Request.Builder().url(DEVICE_CODE_URL).post(body).build()
            httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Microsoft Device Code Error (${resp.code}): $text")
                }
                val json = JSONObject(text)
                DeviceCodeResponse(
                    deviceCode = json.getString("device_code"),
                    userCode = json.getString("user_code"),
                    verificationUri = json.optString(
                        "verification_uri",
                        "https://www.microsoft.com/link",
                    ),
                    expiresIn = json.optInt("expires_in", 900),
                    interval = json.optInt("interval", 5),
                )
            }
        }

    suspend fun pollAndCompleteMinecraftAuth(
        clientId: String,
        deviceCode: DeviceCodeResponse,
    ): MinecraftAuthResult = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + deviceCode.expiresIn * 1000L
        var msAccessToken: String? = null
        var msRefreshToken = ""

        while (System.currentTimeMillis() < deadline && msAccessToken == null) {
            delay(deviceCode.interval.coerceAtLeast(3) * 1000L)
            val body = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("client_id", clientId)
                .add("device_code", deviceCode.deviceCode)
                .build()
            val req = Request.Builder().url(TOKEN_URL).post(body).build()
            httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = JSONObject(text)
                if (resp.isSuccessful) {
                    msAccessToken = json.getString("access_token")
                    msRefreshToken = json.optString("refresh_token", "")
                } else {
                    val err = json.optString("error", "unknown")
                    if (err != "authorization_pending" && err != "slow_down") {
                        throw IllegalStateException("فشل تسجيل الدخول: $err")
                    }
                }
            }
        }

        val token = msAccessToken
            ?: throw IllegalStateException("انتهت مهلة رمز التحقق من مايكروسوفت")

        val (xblToken, userHash) = authenticateXboxLive(token)
        val xstsToken = authorizeXsts(xblToken)
        val mcToken = loginMinecraftWithXbox(userHash, xstsToken)
        val (username, uuid) = fetchMinecraftProfile(mcToken)

        MinecraftAuthResult(
            username = username,
            uuid = uuid,
            mcAccessToken = mcToken,
            msRefreshToken = msRefreshToken,
        )
    }

    private fun authenticateXboxLive(msAccessToken: String): Pair<String, String> {
        val payload = JSONObject().apply {
            put(
                "Properties",
                JSONObject().apply {
                    put("AuthMethod", "RPS")
                    put("SiteName", "user.auth.xboxlive.com")
                    put("RpsTicket", "d=$msAccessToken")
                },
            )
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }
        val req = Request.Builder()
            .url(XBL_AUTH_URL)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("Xbox Live Error (${resp.code})")
            val json = JSONObject(text)
            val xblToken = json.getString("Token")
            val uhs = json.getJSONObject("DisplayClaims")
                .getJSONArray("xui")
                .getJSONObject(0)
                .getString("uhs")
            return xblToken to uhs
        }
    }

    private fun authorizeXsts(xblToken: String): String {
        val payload = JSONObject().apply {
            put(
                "Properties",
                JSONObject().apply {
                    put("SandboxId", "RETAIL")
                    put("UserTokens", JSONArray().put(xblToken))
                },
            )
            put("RelyingParty", "rp://api.minecraftservices.com/")
            put("TokenType", "JWT")
        }
        val req = Request.Builder()
            .url(XSTS_AUTH_URL)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("XSTS Error (${resp.code})")
            return JSONObject(text).getString("Token")
        }
    }

    private fun loginMinecraftWithXbox(userHash: String, xstsToken: String): String {
        val payload = JSONObject().apply {
            put("identityToken", "XBL3.0 x=$userHash;$xstsToken")
        }
        val req = Request.Builder()
            .url(MC_LOGIN_URL)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Minecraft Auth Error (${resp.code})")
            }
            return JSONObject(text).getString("access_token")
        }
    }

    private fun fetchMinecraftProfile(mcToken: String): Pair<String, String> {
        val req = Request.Builder()
            .url(MC_PROFILE_URL)
            .addHeader("Authorization", "Bearer $mcToken")
            .get()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("لا يوجد ملف ماين كرافت مملوك على هذا الحساب (${resp.code})")
            }
            val json = JSONObject(text)
            return json.getString("name") to json.getString("id")
        }
    }

    suspend fun uploadOfficialSkin(
        mcToken: String,
        variant: String, // "classic" or "slim"
        pngBytes: ByteArray,
    ): Boolean = withContext(Dispatchers.IO) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("variant", variant)
            .addFormDataPart(
                "file",
                "skin.png",
                pngBytes.toRequestBody("image/png".toMediaType()),
            )
            .build()
        val req = Request.Builder()
            .url(MC_SKINS_URL)
            .addHeader("Authorization", "Bearer $mcToken")
            .post(multipart)
            .build()
        httpClient.newCall(req).execute().use { it.isSuccessful }
    }
}

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "RustCraft-Launcher/1.0.0 (Android)")
                .build()
            chain.proceed(req)
        }
        .build()

    val mojangApi: MojangApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://launchermeta.mojang.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MojangApiService::class.java)
    }

    val fabricMetaApi: FabricMetaService by lazy {
        Retrofit.Builder()
            .baseUrl("https://meta.fabricmc.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FabricMetaService::class.java)
    }

    val modrinthApi: ModrinthApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.modrinth.com/v2/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ModrinthApiService::class.java)
    }

    val curseForgeApi: CurseForgeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.curseforge.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CurseForgeApiService::class.java)
    }

    val microsoftAuthClient: MicrosoftAuthClient by lazy {
        MicrosoftAuthClient(okHttpClient)
    }

    fun getCurseForgeApiKey(): String {
        val raw = BuildConfig.CURSEFORGE_API_KEY.trim()
        return if (raw.isEmpty() || raw == "YOUR_CURSEFORGE_API_KEY") "" else raw
    }
}
