package id.temankontrol.server

import java.io.File

data class ServerConfig(
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val dbPath: String = System.getenv("DB_PATH") ?: "/data/db/temankontrol.db",
    val mediaDir: File = File(System.getenv("MEDIA_DIR") ?: "/data/attachments"),
    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "dev-only-change-in-production-temankontrol-2026",
    val accessTokenTtlSeconds: Long = System.getenv("ACCESS_TOKEN_TTL_SEC")?.toLongOrNull() ?: 900L,
    val refreshTokenTtlMillis: Long = (System.getenv("REFRESH_TOKEN_TTL_DAYS")?.toLongOrNull() ?: 30L) * 86_400_000L,
    val pbkdf2Iterations: Int = System.getenv("PBKDF2_ITERATIONS")?.toIntOrNull() ?: 120_000,
    val maxAttachmentBytes: Long = 15L * 1024L * 1024L
)
