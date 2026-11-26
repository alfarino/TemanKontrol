package id.temankontrol.app.data.remote

import id.temankontrol.shared.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.Closeable

class CloudApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String
) : Exception(message)

class TemanKontrolApiClient(
    engine: HttpClientEngine = OkHttp.create(),
    private val defaultBaseUrl: String = DEFAULT_BASE_URL
) : Closeable {

    companion object {
        const val DEFAULT_BASE_URL = "https://temankontrol.unand.tech"
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

    private fun url(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return "$trimmed$path"
    }

    suspend fun health(baseUrl: String = defaultBaseUrl): HealthResponse {
        val response = client.get(url(baseUrl, "/health"))
        ensureSuccess(response)
        return response.body()
    }

    suspend fun register(request: RegisterRequest, baseUrl: String = defaultBaseUrl): AuthResponse {
        val response = client.post(url(baseUrl, "/v1/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun login(request: LoginRequest, baseUrl: String = defaultBaseUrl): AuthResponse {
        val response = client.post(url(baseUrl, "/v1/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun refresh(refreshToken: String, baseUrl: String = defaultBaseUrl): AuthResponse {
        val response = client.post(url(baseUrl, "/v1/auth/refresh")) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun logout(refreshToken: String, baseUrl: String = defaultBaseUrl) {
        runCatching {
            client.post(url(baseUrl, "/v1/auth/logout")) {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequest(refreshToken))
            }
        }
    }

    suspend fun forgotPassword(email: String, baseUrl: String = defaultBaseUrl): OtpDispatchResponse {
        val response = client.post(url(baseUrl, "/v1/auth/forgot-password")) {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun resetPassword(request: ResetPasswordRequest, baseUrl: String = defaultBaseUrl) {
        val response = client.post(url(baseUrl, "/v1/auth/reset-password")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        ensureSuccess(response)
    }

    suspend fun deleteAccount(
        accessToken: String,
        password: String,
        baseUrl: String = defaultBaseUrl
    ) {
        val response = client.delete(url(baseUrl, "/v1/account")) {
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
        val response = client.post(url(baseUrl, "/v1/sync/push")) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun pullSync(
        accessToken: String,
        sinceMillis: Long,
        baseUrl: String = defaultBaseUrl
    ): SyncPullResponse {
        val response = client.get(url(baseUrl, "/v1/sync/pull?since=$sinceMillis")) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun uploadAttachmentFile(
        accessToken: String,
        attachmentId: String,
        bytes: ByteArray,
        baseUrl: String = defaultBaseUrl
    ) {
        val response = client.put(url(baseUrl, "/v1/attachments/$attachmentId/file")) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(bytes)
        }
        ensureSuccess(response)
    }

    suspend fun downloadAttachmentFile(
        accessToken: String,
        attachmentId: String,
        baseUrl: String = defaultBaseUrl
    ): ByteArray? {
        val response = client.get(url(baseUrl, "/v1/attachments/$attachmentId/file")) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        ensureSuccess(response)
        return response.readRawBytes()
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching { json.decodeFromString<ApiErrorDto>(rawBody) }.getOrNull()
        throw CloudApiException(
            statusCode = response.status.value,
            errorCode = parsed?.code ?: "HTTP_${response.status.value}",
            message = parsed?.message ?: "Gagal terhubung ke server (${response.status.value})."
        )
    }

    override fun close() {
        client.close()
    }
}
