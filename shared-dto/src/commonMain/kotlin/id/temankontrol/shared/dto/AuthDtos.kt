package id.temankontrol.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val database: String,
    val journalMode: String,
    val serverTimeMillis: Long
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val privacyPolicyAccepted: Boolean
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceName: String? = null
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val otpCode: String,
    val newPassword: String
)

@Serializable
data class DeleteAccountRequest(
    val password: String
)

@Serializable
data class OtpDispatchResponse(
    val sent: Boolean,
    val expiresInMinutes: Int,
    val message: String
)

@Serializable
data class UserProfileDto(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAtMillis: Long
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val user: UserProfileDto
)
