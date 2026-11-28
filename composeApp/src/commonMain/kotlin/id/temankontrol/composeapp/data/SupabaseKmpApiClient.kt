package id.temankontrol.composeapp.data

import id.temankontrol.composeapp.platform.createHttpClientEngine
import id.temankontrol.composeapp.platform.currentTimeMillis
import id.temankontrol.shared.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class KmpCloudApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String
) : Exception(message)

@Serializable
private data class KmpSupabaseUserMetadata(
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
private data class KmpSupabaseSignUpBody(
    val email: String,
    val password: String,
    val data: KmpSupabaseUserMetadata
)

@Serializable
private data class KmpSupabaseLoginBody(
    val email: String,
    val password: String
)

@Serializable
private data class KmpSupabaseRefreshBody(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
private data class KmpSupabaseRecoverBody(
    val email: String
)

@Serializable
private data class KmpSupabaseVerifyRecoveryBody(
    val type: String,
    val email: String,
    val token: String
)

@Serializable
private data class KmpSupabaseUpdatePasswordBody(
    val password: String
)

@Serializable
private data class KmpSupabasePushRpcBody(
    val payload: SyncPushRequest
)

@Serializable
private data class KmpSupabasePullRpcBody(
    @SerialName("since_millis") val sinceMillis: Long
)

@Serializable
private data class KmpSupabaseUserBody(
    val id: String,
    val email: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_metadata") val userMetadata: KmpSupabaseUserMetadata? = null
)

@Serializable
private data class KmpSupabaseAuthBody(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600L,
    val user: KmpSupabaseUserBody? = null
)

class SupabaseKmpApiClient(
    engine: HttpClientEngine = createHttpClientEngine(),
    private val defaultBaseUrl: String = DEFAULT_BASE_URL,
    private val defaultApiKey: String = DEFAULT_ANON_KEY
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://hqtrpbagnosmrznjiiph.supabase.co"
        const val DEFAULT_ANON_KEY = "sb_publishable_seW-ToTABtcx7-lWasypgA_nZ13ij3p"
        private const val LEGACY_PRIVATE_HOST = "unand.tech"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun resolveBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isBlank() || trimmed.contains(LEGACY_PRIVATE_HOST, ignoreCase = true)) {
            return defaultBaseUrl.trimEnd('/')
        }
        return trimmed
    }

    private fun url(baseUrl: String, path: String): String = "${resolveBaseUrl(baseUrl)}$path"

    suspend fun health(baseUrl: String = defaultBaseUrl): HealthResponse {
        val response = client.get(url(baseUrl, "/auth/v1/health")) {
            header("apikey", defaultApiKey)
        }
        ensureSuccess(response)
        return HealthResponse(
            status = "UP",
            service = "supabase-temankontrol",
            database = "postgres",
            journalMode = "rls",
            serverTimeMillis = currentTimeMillis()
        )
    }

    suspend fun register(request: RegisterRequest, baseUrl: String = defaultBaseUrl): AuthResponse {
        val email = request.email.trim().lowercase()
        val displayName = request.displayName.trim()
        if (!email.contains("@") || !email.contains(".")) {
            throw KmpCloudApiException(400, "INVALID_EMAIL", "Format email tidak valid.")
        }
        if (request.password.length < 8) {
            throw KmpCloudApiException(400, "WEAK_PASSWORD", "Kata sandi minimal 8 karakter.")
        }
        if (displayName.isBlank()) {
            throw KmpCloudApiException(400, "INVALID_NAME", "Nama tampilan wajib diisi.")
        }
        if (!request.privacyPolicyAccepted) {
            throw KmpCloudApiException(400, "PRIVACY_REQUIRED", "Persetujuan Kebijakan Privasi wajib dicentang.")
        }

        val response = client.post(url(baseUrl, "/auth/v1/signup")) {
            header("apikey", defaultApiKey)
            contentType(ContentType.Application.Json)
            setBody(
                KmpSupabaseSignUpBody(
                    email = email,
                    password = request.password,
                    data = KmpSupabaseUserMetadata(displayName = displayName)
                )
            )
        }
        ensureSuccess(response)
        val body: KmpSupabaseAuthBody = response.body()
        if (body.accessToken.isNullOrBlank() || body.refreshToken.isNullOrBlank() || body.user == null) {
            throw KmpCloudApiException(
                statusCode = 400,
                errorCode = "EMAIL_CONFIRMATION_REQUIRED",
                message = "Akun dibuat! Silakan cek kotak masuk email Anda untuk konfirmasi lalu masuk."
            )
        }
        return body.toAuthResponse(fallbackDisplayName = displayName)
    }

    suspend fun login(request: LoginRequest, baseUrl: String = defaultBaseUrl): AuthResponse {
        val email = request.email.trim().lowercase()
        val response = client.post(url(baseUrl, "/auth/v1/token?grant_type=password")) {
            header("apikey", defaultApiKey)
            contentType(ContentType.Application.Json)
            setBody(KmpSupabaseLoginBody(email = email, password = request.password))
        }
        ensureSuccess(response)
        val body: KmpSupabaseAuthBody = response.body()
        return body.toAuthResponse()
    }

    suspend fun refresh(refreshToken: String, baseUrl: String = defaultBaseUrl): AuthResponse {
        val response = client.post(url(baseUrl, "/auth/v1/token?grant_type=refresh_token")) {
            header("apikey", defaultApiKey)
            contentType(ContentType.Application.Json)
            setBody(KmpSupabaseRefreshBody(refreshToken))
        }
        ensureSuccess(response)
        val body: KmpSupabaseAuthBody = response.body()
        return body.toAuthResponse()
    }

    suspend fun logout(accessToken: String, baseUrl: String = defaultBaseUrl) {
        runCatching {
            client.post(url(baseUrl, "/auth/v1/logout")) {
                header("apikey", defaultApiKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun forgotPassword(email: String, baseUrl: String = defaultBaseUrl): OtpDispatchResponse {
        val response = client.post(url(baseUrl, "/auth/v1/recover")) {
            header("apikey", defaultApiKey)
            contentType(ContentType.Application.Json)
            setBody(KmpSupabaseRecoverBody(email.trim().lowercase()))
        }
        ensureSuccess(response)
        return OtpDispatchResponse(
            sent = true,
            expiresInMinutes = 15,
            message = "Jika email terdaftar, kode OTP pemulihan telah dikirim ke email Anda."
        )
    }

    suspend fun resetPassword(request: ResetPasswordRequest, baseUrl: String = defaultBaseUrl) {
        if (request.newPassword.length < 8) {
            throw KmpCloudApiException(400, "WEAK_PASSWORD", "Kata sandi baru minimal 8 karakter.")
        }
        val verifyRes = client.post(url(baseUrl, "/auth/v1/verify")) {
            header("apikey", defaultApiKey)
            contentType(ContentType.Application.Json)
            setBody(
                KmpSupabaseVerifyRecoveryBody(
                    type = "recovery",
                    email = request.email.trim().lowercase(),
                    token = request.otpCode.trim()
                )
            )
        }
        ensureSuccess(verifyRes)
        val recoveryAuth: KmpSupabaseAuthBody = verifyRes.body()
        val recoveryToken = recoveryAuth.accessToken
            ?: throw KmpCloudApiException(400, "INVALID_OTP", "Kode OTP salah atau sudah kedaluwarsa.")

        val updateRes = client.put(url(baseUrl, "/auth/v1/user")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $recoveryToken")
            contentType(ContentType.Application.Json)
            setBody(KmpSupabaseUpdatePasswordBody(request.newPassword))
        }
        ensureSuccess(updateRes)
        logout(recoveryToken, baseUrl)
    }

    suspend fun deleteAccount(
        accessToken: String,
        password: String,
        baseUrl: String = defaultBaseUrl
    ) {
        val response = client.post(url(baseUrl, "/functions/v1/delete-account")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(DeleteAccountRequest(password))
        }
        ensureSuccess(response)
    }

    suspend fun pushSync(
        accessToken: String,
        request: SyncPushRequest,
        baseUrl: String = defaultBaseUrl
    ): SyncPushResponse {
        val response = client.post(url(baseUrl, "/rest/v1/rpc/sync_push")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(KmpSupabasePushRpcBody(request))
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun pullSync(
        accessToken: String,
        sinceMillis: Long,
        baseUrl: String = defaultBaseUrl
    ): SyncPullResponse {
        val response = client.post(url(baseUrl, "/rest/v1/rpc/sync_pull")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(KmpSupabasePullRpcBody(sinceMillis))
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun uploadAttachmentFile(
        accessToken: String,
        userId: String,
        attachmentId: String,
        bytes: ByteArray,
        mimeType: String = "application/octet-stream",
        baseUrl: String = defaultBaseUrl
    ) {
        val response = client.post(url(baseUrl, "/storage/v1/object/attachments/$userId/$attachmentId")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("x-upsert", "true")
            contentType(runCatching { ContentType.parse(mimeType) }.getOrDefault(ContentType.Application.OctetStream))
            setBody(bytes)
        }
        ensureSuccess(response)
    }

    suspend fun downloadAttachmentFile(
        accessToken: String,
        userId: String,
        attachmentId: String,
        baseUrl: String = defaultBaseUrl
    ): ByteArray? {
        val response = client.get(url(baseUrl, "/storage/v1/object/attachments/$userId/$attachmentId")) {
            header("apikey", defaultApiKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (response.status == HttpStatusCode.BadRequest) {
            val errBody = runCatching { response.bodyAsText() }.getOrDefault("")
            if (errBody.contains("not_found", ignoreCase = true) ||
                errBody.contains("Object not found", ignoreCase = true)
            ) {
                return null
            }
        }
        ensureSuccess(response)
        return response.readRawBytes()
    }

    private fun KmpSupabaseAuthBody.toAuthResponse(fallbackDisplayName: String = "Pengguna"): AuthResponse {
        val u = user ?: throw KmpCloudApiException(500, "INVALID_AUTH_RESPONSE", "Respons akun tidak lengkap.")
        val token = accessToken ?: throw KmpCloudApiException(401, "NO_ACCESS_TOKEN", "Sesi akses tidak ditemukan.")
        val refresh = refreshToken ?: throw KmpCloudApiException(401, "NO_REFRESH_TOKEN", "Token pembaruan tidak ditemukan.")
        val display = u.userMetadata?.displayName?.takeIf { it.isNotBlank() }
            ?: u.email?.substringBefore("@")
            ?: fallbackDisplayName
        return AuthResponse(
            accessToken = token,
            refreshToken = refresh,
            expiresInSeconds = expiresIn,
            user = UserProfileDto(
                userId = u.id,
                email = u.email.orEmpty(),
                displayName = display,
                createdAtMillis = currentTimeMillis()
            )
        )
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsedObj = runCatching { json.decodeFromString<JsonObject>(rawBody) }.getOrNull()
        val code = parsedObj?.get("error_code")?.jsonPrimitive?.contentOrNull
            ?: parsedObj?.get("code")?.jsonPrimitive?.contentOrNull
            ?: parsedObj?.get("error")?.jsonPrimitive?.contentOrNull
            ?: "HTTP_${response.status.value}"
        val rawMsg = parsedObj?.get("msg")?.jsonPrimitive?.contentOrNull
            ?: parsedObj?.get("message")?.jsonPrimitive?.contentOrNull
            ?: parsedObj?.get("error_description")?.jsonPrimitive?.contentOrNull
            ?: "Gagal terhubung ke server (${response.status.value})."

        throw KmpCloudApiException(
            statusCode = response.status.value,
            errorCode = code,
            message = mapErrorMessage(code, rawMsg, response.status.value)
        )
    }

    private fun mapErrorMessage(code: String, rawMsg: String, statusCode: Int): String {
        val lowerCode = code.lowercase()
        val lowerMsg = rawMsg.lowercase()
        return when {
            lowerCode == "invalid_credentials" || lowerMsg.contains("invalid login credentials") ->
                "Email atau kata sandi salah."
            lowerCode == "user_already_exists" || lowerMsg.contains("already registered") ->
                "Email sudah terdaftar."
            lowerCode == "email_not_confirmed" || lowerMsg.contains("email not confirmed") ->
                "Email belum diverifikasi. Silakan cek kotak masuk email Anda."
            lowerCode == "otp_expired" || lowerMsg.contains("token has expired") || lowerMsg.contains("invalid token") ->
                "Kode OTP salah atau sudah kedaluwarsa."
            lowerCode == "weak_password" || lowerMsg.contains("password should be at least") ->
                "Kata sandi terlalu lemah (minimal 8 karakter)."
            lowerCode == "over_email_send_rate_limit" || statusCode == 429 ->
                "Terlalu banyak permintaan. Tunggu sebentar lalu coba lagi."
            lowerCode == "invalid_password" ->
                "Kata sandi konfirmasi salah."
            else -> rawMsg
        }
    }
}
