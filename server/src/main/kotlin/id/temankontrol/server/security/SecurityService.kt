package id.temankontrol.server.security

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
private data class JwtHeader(
    val alg: String = "HS256",
    val typ: String = "JWT"
)

@Serializable
data class JwtClaims(
    val sub: String,
    val email: String,
    val iat: Long,
    val exp: Long
)

class SecurityService(
    private val jwtSecret: String,
    private val accessTokenTtlSeconds: Long,
    private val pbkdf2Iterations: Int
) {
    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }
    private val b64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val b64Decoder = Base64.getUrlDecoder()

    fun hashPassword(password: String): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val derived = pbkdf2(password, salt, pbkdf2Iterations)
        return "$pbkdf2Iterations:${b64Encoder.encodeToString(salt)}:${b64Encoder.encodeToString(derived)}"
    }

    fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = runCatching { b64Decoder.decode(parts[1]) }.getOrNull() ?: return false
        val expected = runCatching { b64Decoder.decode(parts[2]) }.getOrNull() ?: return false
        val actual = pbkdf2(password, salt, iterations)
        return MessageDigest.isEqual(actual, expected)
    }

    fun generateOpaqueToken(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return b64Encoder.encodeToString(bytes)
    }

    fun generateSixDigitOtp(): String {
        val num = random.nextInt(900_000) + 100_000
        return num.toString()
    }

    fun sha256Hex(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun issueAccessToken(userId: String, email: String, nowSeconds: Long = System.currentTimeMillis() / 1000L): String {
        val headerPart = b64Encoder.encodeToString(json.encodeToString(JwtHeader()).toByteArray(StandardCharsets.UTF_8))
        val claims = JwtClaims(
            sub = userId,
            email = email,
            iat = nowSeconds,
            exp = nowSeconds + accessTokenTtlSeconds
        )
        val payloadPart = b64Encoder.encodeToString(json.encodeToString(claims).toByteArray(StandardCharsets.UTF_8))
        val signingInput = "$headerPart.$payloadPart"
        val signature = signHmacSha256(signingInput)
        return "$signingInput.$signature"
    }

    fun verifyAccessToken(token: String, nowSeconds: Long = System.currentTimeMillis() / 1000L): JwtClaims? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val signingInput = "${parts[0]}.${parts[1]}"
        val expectedSig = signHmacSha256(signingInput)
        if (!MessageDigest.isEqual(
                parts[2].toByteArray(StandardCharsets.UTF_8),
                expectedSig.toByteArray(StandardCharsets.UTF_8)
            )
        ) {
            return null
        }
        val claimsJson = runCatching {
            String(b64Decoder.decode(parts[1]), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        val claims = runCatching { json.decodeFromString<JwtClaims>(claimsJson) }.getOrNull() ?: return null
        if (claims.exp < nowSeconds) return null
        return claims
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun signHmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(jwtSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return b64Encoder.encodeToString(mac.doFinal(data.toByteArray(StandardCharsets.UTF_8)))
    }
}
