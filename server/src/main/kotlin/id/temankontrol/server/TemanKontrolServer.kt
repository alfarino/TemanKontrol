package id.temankontrol.server

import id.temankontrol.server.db.SqliteDatabaseManager
import id.temankontrol.server.repository.AuthAndSyncRepository
import id.temankontrol.server.repository.AuthResult
import id.temankontrol.server.security.JwtClaims
import id.temankontrol.server.security.SecurityService
import id.temankontrol.shared.dto.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val config = ServerConfig()
    embeddedServer(Netty, port = config.port) {
        temanKontrolModule(config)
    }.start(wait = true)
}

fun Application.temanKontrolModule(
    config: ServerConfig = ServerConfig(),
    onOtpGeneratedForTest: ((email: String, otp: String) -> Unit)? = null
) {
    config.mediaDir.mkdirs()
    val db = SqliteDatabaseManager(config.dbPath)
    val security = SecurityService(
        jwtSecret = config.jwtSecret,
        accessTokenTtlSeconds = config.accessTokenTtlSeconds,
        pbkdf2Iterations = config.pbkdf2Iterations
    )
    val repo = AuthAndSyncRepository(
        db = db,
        security = security,
        mediaDir = config.mediaDir,
        accessTokenTtlSeconds = config.accessTokenTtlSeconds,
        refreshTokenTtlMillis = config.refreshTokenTtlMillis
    )

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CallLogging)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorDto("INTERNAL_ERROR", cause.message ?: "Terjadi kesalahan pada server.")
            )
        }
    }

    suspend fun ApplicationCall.requireClaims(): JwtClaims? {
        val header = request.header(HttpHeaders.Authorization)
        val token = header?.removePrefix("Bearer ")?.trim()
        if (token.isNullOrBlank()) {
            respond(HttpStatusCode.Unauthorized, ApiErrorDto("UNAUTHORIZED", "Token otorisasi diperlukan."))
            return null
        }
        val claims = security.verifyAccessToken(token)
        if (claims == null) {
            respond(HttpStatusCode.Unauthorized, ApiErrorDto("TOKEN_EXPIRED", "Token tidak valid atau kedaluwarsa."))
            return null
        }
        return claims
    }

    routing {
        get("/health") {
            call.respond(
                HealthResponse(
                    status = "UP",
                    service = "temankontrol-api",
                    database = "sqlite",
                    journalMode = db.journalMode,
                    serverTimeMillis = System.currentTimeMillis()
                )
            )
        }

        get("/privacy") {
            call.respondText(PRIVACY_POLICY_HTML, ContentType.Text.Html)
        }

        route("/v1") {
            route("/auth") {
                post("/register") {
                    val req = call.receive<RegisterRequest>()
                    when (val res = repo.register(req)) {
                        is AuthResult.Ok -> call.respond(HttpStatusCode.Created, res.data)
                        is AuthResult.Err -> call.respond(HttpStatusCode.fromValue(res.statusCode), ApiErrorDto(res.code, res.message))
                    }
                }

                post("/login") {
                    val req = call.receive<LoginRequest>()
                    when (val res = repo.login(req)) {
                        is AuthResult.Ok -> call.respond(HttpStatusCode.OK, res.data)
                        is AuthResult.Err -> call.respond(HttpStatusCode.fromValue(res.statusCode), ApiErrorDto(res.code, res.message))
                    }
                }

                post("/refresh") {
                    val req = call.receive<RefreshRequest>()
                    when (val res = repo.refresh(req)) {
                        is AuthResult.Ok -> call.respond(HttpStatusCode.OK, res.data)
                        is AuthResult.Err -> call.respond(HttpStatusCode.fromValue(res.statusCode), ApiErrorDto(res.code, res.message))
                    }
                }

                post("/logout") {
                    val req = call.receive<LogoutRequest>()
                    repo.logout(req)
                    call.respond(HttpStatusCode.NoContent)
                }

                post("/forgot-password") {
                    val req = call.receive<ForgotPasswordRequest>()
                    val otp = repo.createPasswordResetOtp(req.email)
                    if (otp != null) {
                        onOtpGeneratedForTest?.invoke(req.email.trim().lowercase(), otp)
                    }
                    // Always return 200 to prevent account enumeration
                    call.respond(
                        HttpStatusCode.OK,
                        OtpDispatchResponse(
                            sent = true,
                            expiresInMinutes = 15,
                            message = "Jika email terdaftar, kode OTP pemulihan telah dikirim."
                        )
                    )
                }

                post("/reset-password") {
                    val req = call.receive<ResetPasswordRequest>()
                    when (val res = repo.resetPassword(req)) {
                        is AuthResult.Ok -> call.respond(HttpStatusCode.OK, mapOf("reset" to true))
                        is AuthResult.Err -> call.respond(HttpStatusCode.fromValue(res.statusCode), ApiErrorDto(res.code, res.message))
                    }
                }
            }

            delete("/account") {
                val claims = call.requireClaims() ?: return@delete
                val req = call.receive<DeleteAccountRequest>()
                when (val res = repo.deleteAccountPermanently(claims.sub, req.password)) {
                    is AuthResult.Ok -> call.respond(HttpStatusCode.NoContent)
                    is AuthResult.Err -> call.respond(HttpStatusCode.fromValue(res.statusCode), ApiErrorDto(res.code, res.message))
                }
            }

            post("/sync/push") {
                val claims = call.requireClaims() ?: return@post
                val req = call.receive<SyncPushRequest>()
                val result = repo.pushSync(claims.sub, req)
                call.respond(HttpStatusCode.OK, result)
            }

            get("/sync/pull") {
                val claims = call.requireClaims() ?: return@get
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val result = repo.pullSync(claims.sub, since)
                call.respond(HttpStatusCode.OK, result)
            }

            put("/attachments/{attachmentId}/file") {
                val claims = call.requireClaims() ?: return@put
                val attachmentId = call.parameters["attachmentId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiErrorDto("INVALID_ID", "ID lampiran tidak valid.")
                )
                val bytes = call.receive<ByteArray>()
                if (bytes.size > config.maxAttachmentBytes) {
                    return@put call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ApiErrorDto("FILE_TOO_LARGE", "Ukuran file melebihi batas 15 MB.")
                    )
                }
                val targetFile = repo.getUserAttachmentFile(claims.sub, attachmentId)
                targetFile.writeBytes(bytes)
                call.respond(HttpStatusCode.OK, mapOf("storedBytes" to bytes.size))
            }

            get("/attachments/{attachmentId}/file") {
                val claims = call.requireClaims() ?: return@get
                val attachmentId = call.parameters["attachmentId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiErrorDto("INVALID_ID", "ID lampiran tidak valid.")
                )
                val targetFile = repo.getUserAttachmentFile(claims.sub, attachmentId)
                if (!targetFile.exists()) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ApiErrorDto("FILE_NOT_FOUND", "File lampiran tidak ditemukan di server.")
                    )
                }
                call.respondBytes(targetFile.readBytes(), ContentType.Application.OctetStream)
            }
        }
    }
}

private const val PRIVACY_POLICY_HTML = """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Kebijakan Privasi — Teman Kontrol</title>
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; max-width: 760px; margin: 40px auto; padding: 0 20px; line-height: 1.6; color: #141D1E; background: #F3F8F8; }
    .card { background: #fff; border-radius: 16px; padding: 28px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
    h1 { color: #0B6E72; }
    h2 { color: #14919B; margin-top: 24px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Kebijakan Privasi Teman Kontrol</h1>
    <p><em>Diperbarui: 26 November 2026 — Selaras dengan UU PDP No. 27 Tahun 2022</em></p>
    <p>Teman Kontrol adalah aplikasi pendamping persiapan konsultasi kesehatan. Aplikasi ini <strong>bukan alat diagnosis, bukan layanan telemedicine, dan bukan layanan gawat darurat</strong>.</p>
    <h2>1. Data yang Disimpan</h2>
    <p>Jika Anda memilih masuk (login) ke akun cloud <code>api.temankontrol.unand.tech</code>, data berikut disimpan secara aman untuk tujuan pemulihan antar perangkat (Android &amp; iPhone): alamat email, nama tampilan, jadwal kunjungan, pertanyaan terbuka, catatan jawaban/hasil, tindak lanjut, setelan alarm, serta lampiran foto &amp; rekaman suara (VN) yang Anda unggah sendiri.</p>
    <h2>2. Keamanan &amp; Privasi</h2>
    <p>Seluruh komunikasi menggunakan enkripsi HTTPS TLS 1.3 melalui Cloudflare Zero Trust Tunnel. Kata sandi di-hash menggunakan PBKDF2WithHmacSHA256 + random salt. Data tidak pernah dijual atau dibagikan kepada pihak ketiga.</p>
    <h2>3. Hak Hapus Akun &amp; Data Permanen</h2>
    <p>Anda dapat menghapus akun beserta seluruh catatan dan file foto/VN secara permanen kapan saja melalui menu <strong>Akun &rarr; Hapus Akun &amp; Data Permanen</strong> di dalam aplikasi.</p>
  </div>
</body>
</html>
"""
