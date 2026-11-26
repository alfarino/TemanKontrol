package id.temankontrol.server.repository

import id.temankontrol.server.db.SqliteDatabaseManager
import id.temankontrol.server.security.SecurityService
import id.temankontrol.shared.dto.*
import java.io.File
import java.sql.Connection
import java.sql.Types
import java.util.UUID

sealed interface AuthResult<out T> {
    data class Ok<T>(val data: T) : AuthResult<T>
    data class Err(val statusCode: Int, val code: String, val message: String) : AuthResult<Nothing>
}

class AuthAndSyncRepository(
    private val db: SqliteDatabaseManager,
    private val security: SecurityService,
    private val mediaDir: File,
    private val accessTokenTtlSeconds: Long,
    private val refreshTokenTtlMillis: Long
) {

    fun register(req: RegisterRequest): AuthResult<AuthResponse> {
        val email = req.email.trim().lowercase()
        val displayName = req.displayName.trim()
        if (!email.contains("@") || !email.contains(".")) {
            return AuthResult.Err(400, "INVALID_EMAIL", "Format email tidak valid.")
        }
        if (req.password.length < 8) {
            return AuthResult.Err(400, "WEAK_PASSWORD", "Kata sandi minimal 8 karakter.")
        }
        if (displayName.isBlank()) {
            return AuthResult.Err(400, "INVALID_NAME", "Nama tampilan wajib diisi.")
        }
        if (!req.privacyPolicyAccepted) {
            return AuthResult.Err(400, "PRIVACY_REQUIRED", "Persetujuan Kebijakan Privasi wajib dicentang.")
        }

        val nowMs = System.currentTimeMillis()
        val userId = UUID.randomUUID().toString()
        val passwordHash = security.hashPassword(req.password)

        return db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val exists = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?").use { ps ->
                    ps.setString(1, email)
                    ps.executeQuery().next()
                }
                if (exists) {
                    conn.rollback()
                    return@use AuthResult.Err(409, "EMAIL_EXISTS", "Email sudah terdaftar.")
                }

                conn.prepareStatement(
                    "INSERT INTO users(id, email, password_hash, display_name, created_at) VALUES (?, ?, ?, ?, ?)"
                ).use { ps ->
                    ps.setString(1, userId)
                    ps.setString(2, email)
                    ps.setString(3, passwordHash)
                    ps.setString(4, displayName)
                    ps.setLong(5, nowMs)
                    ps.executeUpdate()
                }

                val refreshToken = insertRefreshToken(conn, userId, deviceName = "Initial Register", nowMs = nowMs)
                conn.commit()

                AuthResult.Ok(
                    AuthResponse(
                        accessToken = security.issueAccessToken(userId, email),
                        refreshToken = refreshToken,
                        expiresInSeconds = accessTokenTtlSeconds,
                        user = UserProfileDto(userId, email, displayName, nowMs)
                    )
                )
            } catch (e: Exception) {
                conn.rollback()
                AuthResult.Err(500, "INTERNAL_ERROR", "Gagal mendaftarkan akun.")
            }
        }
    }

    fun login(req: LoginRequest): AuthResult<AuthResponse> {
        val email = req.email.trim().lowercase()
        val nowMs = System.currentTimeMillis()
        return db.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT id, email, password_hash, display_name, created_at FROM users WHERE email = ?"
            ).use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return@use AuthResult.Err(401, "INVALID_CREDENTIALS", "Email atau kata sandi salah.")
                    }
                    val userId = rs.getString("id")
                    val storedHash = rs.getString("password_hash")
                    val displayName = rs.getString("display_name")
                    val createdAt = rs.getLong("created_at")
                    if (!security.verifyPassword(req.password, storedHash)) {
                        return@use AuthResult.Err(401, "INVALID_CREDENTIALS", "Email atau kata sandi salah.")
                    }
                    val refreshToken = insertRefreshToken(conn, userId, req.deviceName, nowMs)
                    AuthResult.Ok(
                        AuthResponse(
                            accessToken = security.issueAccessToken(userId, email),
                            refreshToken = refreshToken,
                            expiresInSeconds = accessTokenTtlSeconds,
                            user = UserProfileDto(userId, email, displayName, createdAt)
                        )
                    )
                }
            }
        }
    }

    fun refresh(req: RefreshRequest): AuthResult<AuthResponse> {
        val nowMs = System.currentTimeMillis()
        val tokenHash = security.sha256Hex(req.refreshToken)
        return db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                var userId: String? = null
                var email: String? = null
                var displayName: String? = null
                var createdAt = 0L
                var expiresAt = 0L

                conn.prepareStatement(
                    """
                    SELECT rt.user_id, rt.expires_at, u.email, u.display_name, u.created_at
                    FROM refresh_tokens rt
                    JOIN users u ON u.id = rt.user_id
                    WHERE rt.token_hash = ?
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, tokenHash)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            userId = rs.getString("user_id")
                            expiresAt = rs.getLong("expires_at")
                            email = rs.getString("email")
                            displayName = rs.getString("display_name")
                            createdAt = rs.getLong("created_at")
                        }
                    }
                }

                if (userId == null || expiresAt < nowMs) {
                    conn.rollback()
                    return@use AuthResult.Err(401, "INVALID_REFRESH_TOKEN", "Sesi habis, silakan masuk kembali.")
                }

                // Rotate refresh token
                conn.prepareStatement("DELETE FROM refresh_tokens WHERE token_hash = ?").use { ps ->
                    ps.setString(1, tokenHash)
                    ps.executeUpdate()
                }
                val newRefresh = insertRefreshToken(conn, userId!!, deviceName = "Refreshed Session", nowMs = nowMs)
                conn.commit()

                AuthResult.Ok(
                    AuthResponse(
                        accessToken = security.issueAccessToken(userId!!, email!!),
                        refreshToken = newRefresh,
                        expiresInSeconds = accessTokenTtlSeconds,
                        user = UserProfileDto(userId!!, email!!, displayName!!, createdAt)
                    )
                )
            } catch (e: Exception) {
                conn.rollback()
                AuthResult.Err(500, "INTERNAL_ERROR", "Gagal memperbarui sesi.")
            }
        }
    }

    fun logout(req: LogoutRequest) {
        val tokenHash = security.sha256Hex(req.refreshToken)
        db.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM refresh_tokens WHERE token_hash = ?").use { ps ->
                ps.setString(1, tokenHash)
                ps.executeUpdate()
            }
        }
    }

    fun createPasswordResetOtp(emailRaw: String): String? {
        val email = emailRaw.trim().lowercase()
        val nowMs = System.currentTimeMillis()
        val otp = security.generateSixDigitOtp()
        val otpHash = security.sha256Hex(otp)
        return db.getConnection().use { conn ->
            val exists = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().next()
            }
            if (!exists) return@use null

            conn.prepareStatement(
                """
                INSERT INTO otp_codes(email, otp_hash, expires_at, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(email) DO UPDATE SET
                    otp_hash = excluded.otp_hash,
                    expires_at = excluded.expires_at,
                    created_at = excluded.created_at
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, email)
                ps.setString(2, otpHash)
                ps.setLong(3, nowMs + 15 * 60_000L)
                ps.setLong(4, nowMs)
                ps.executeUpdate()
            }
            otp
        }
    }

    fun resetPassword(req: ResetPasswordRequest): AuthResult<Unit> {
        val email = req.email.trim().lowercase()
        if (req.newPassword.length < 8) {
            return AuthResult.Err(400, "WEAK_PASSWORD", "Kata sandi baru minimal 8 karakter.")
        }
        val nowMs = System.currentTimeMillis()
        val expectedHash = security.sha256Hex(req.otpCode.trim())

        return db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                var valid = false
                conn.prepareStatement("SELECT otp_hash, expires_at FROM otp_codes WHERE email = ?").use { ps ->
                    ps.setString(1, email)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val stored = rs.getString("otp_hash")
                            val exp = rs.getLong("expires_at")
                            valid = stored == expectedHash && exp >= nowMs
                        }
                    }
                }
                if (!valid) {
                    conn.rollback()
                    return@use AuthResult.Err(400, "INVALID_OTP", "Kode OTP salah atau sudah kedaluwarsa.")
                }
                val newHash = security.hashPassword(req.newPassword)
                conn.prepareStatement("UPDATE users SET password_hash = ? WHERE email = ?").use { ps ->
                    ps.setString(1, newHash)
                    ps.setString(2, email)
                    ps.executeUpdate()
                }
                conn.prepareStatement("DELETE FROM otp_codes WHERE email = ?").use { ps ->
                    ps.setString(1, email)
                    ps.executeUpdate()
                }
                conn.prepareStatement(
                    "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email = ?)"
                ).use { ps ->
                    ps.setString(1, email)
                    ps.executeUpdate()
                }
                conn.commit()
                AuthResult.Ok(Unit)
            } catch (e: Exception) {
                conn.rollback()
                AuthResult.Err(500, "INTERNAL_ERROR", "Gagal mereset kata sandi.")
            }
        }
    }

    fun deleteAccountPermanently(userId: String, password: String): AuthResult<Unit> {
        return db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val hash = conn.prepareStatement("SELECT password_hash FROM users WHERE id = ?").use { ps ->
                    ps.setString(1, userId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) rs.getString("password_hash") else null
                    }
                } ?: run {
                    conn.rollback()
                    return@use AuthResult.Err(404, "NOT_FOUND", "Akun tidak ditemukan.")
                }

                if (!security.verifyPassword(password, hash)) {
                    conn.rollback()
                    return@use AuthResult.Err(401, "INVALID_PASSWORD", "Kata sandi konfirmasi salah.")
                }

                // CASCADE deletes consultations, questions, notes, follow_ups, attachments, user_settings, refresh_tokens
                conn.prepareStatement("DELETE FROM users WHERE id = ?").use { ps ->
                    ps.setString(1, userId)
                    ps.executeUpdate()
                }
                conn.commit()

                // Remove all encrypted media files on Ubuntu Server volume
                val userMediaDir = File(mediaDir, userId)
                if (userMediaDir.exists()) {
                    userMediaDir.deleteRecursively()
                }
                AuthResult.Ok(Unit)
            } catch (e: Exception) {
                conn.rollback()
                AuthResult.Err(500, "INTERNAL_ERROR", "Gagal menghapus akun.")
            }
        }
    }

    fun pushSync(userId: String, req: SyncPushRequest): SyncPushResponse {
        var accepted = 0
        var skipped = 0
        val serverNow = System.currentTimeMillis()

        db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1. Consultations (LWW based on updated_at)
                req.consultations.forEach { dto ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM consultations WHERE user_id = ? AND id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, dto.id)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > dto.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO consultations(user_id, id, title, appointment_at, reason, facility_name, provider_name, status, created_at, updated_at, deleted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id, id) DO UPDATE SET
                                title = excluded.title,
                                appointment_at = excluded.appointment_at,
                                reason = excluded.reason,
                                facility_name = excluded.facility_name,
                                provider_name = excluded.provider_name,
                                status = excluded.status,
                                updated_at = excluded.updated_at,
                                deleted_at = excluded.deleted_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, dto.id)
                            ps.setString(3, dto.title)
                            ps.setLong(4, dto.appointmentAtMillis)
                            ps.setString(5, dto.reason)
                            ps.setNullableString(6, dto.facilityName)
                            ps.setNullableString(7, dto.providerName)
                            ps.setString(8, dto.status)
                            ps.setLong(9, dto.createdAtMillis)
                            ps.setLong(10, dto.updatedAtMillis)
                            ps.setNullableLong(11, dto.deletedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                // 2. Questions
                req.questions.forEach { dto ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM questions WHERE user_id = ? AND id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, dto.id)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > dto.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO questions(user_id, id, consultation_id, text, is_priority, is_discussed, created_at, updated_at, answer, answer_updated_at, deleted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id, id) DO UPDATE SET
                                text = excluded.text,
                                is_priority = excluded.is_priority,
                                is_discussed = excluded.is_discussed,
                                updated_at = excluded.updated_at,
                                answer = excluded.answer,
                                answer_updated_at = excluded.answer_updated_at,
                                deleted_at = excluded.deleted_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, dto.id)
                            ps.setString(3, dto.consultationId)
                            ps.setString(4, dto.text)
                            ps.setInt(5, if (dto.isPriority) 1 else 0)
                            ps.setInt(6, if (dto.isDiscussed) 1 else 0)
                            ps.setLong(7, dto.createdAtMillis)
                            ps.setLong(8, dto.updatedAtMillis)
                            ps.setNullableString(9, dto.answer)
                            ps.setNullableLong(10, dto.answerUpdatedAtMillis)
                            ps.setNullableLong(11, dto.deletedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                // 3. Consultation Notes
                req.notes.forEach { dto ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM consultation_notes WHERE user_id = ? AND consultation_id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, dto.consultationId)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > dto.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO consultation_notes(user_id, consultation_id, summary, provider_instruction, additional_notes, updated_at, deleted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id, consultation_id) DO UPDATE SET
                                summary = excluded.summary,
                                provider_instruction = excluded.provider_instruction,
                                additional_notes = excluded.additional_notes,
                                updated_at = excluded.updated_at,
                                deleted_at = excluded.deleted_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, dto.consultationId)
                            ps.setNullableString(3, dto.summary)
                            ps.setNullableString(4, dto.providerInstruction)
                            ps.setNullableString(5, dto.additionalNotes)
                            ps.setLong(6, dto.updatedAtMillis)
                            ps.setNullableLong(7, dto.deletedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                // 4. Follow Up Actions
                req.followUps.forEach { dto ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM follow_up_actions WHERE user_id = ? AND id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, dto.id)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > dto.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO follow_up_actions(user_id, id, consultation_id, text, due_date_epoch_day, is_completed, completed_at, created_at, updated_at, deleted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id, id) DO UPDATE SET
                                text = excluded.text,
                                due_date_epoch_day = excluded.due_date_epoch_day,
                                is_completed = excluded.is_completed,
                                completed_at = excluded.completed_at,
                                updated_at = excluded.updated_at,
                                deleted_at = excluded.deleted_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, dto.id)
                            ps.setString(3, dto.consultationId)
                            ps.setString(4, dto.text)
                            ps.setNullableLong(5, dto.dueDateEpochDay)
                            ps.setInt(6, if (dto.isCompleted) 1 else 0)
                            ps.setNullableLong(7, dto.completedAtMillis)
                            ps.setLong(8, dto.createdAtMillis)
                            ps.setLong(9, dto.updatedAtMillis)
                            ps.setNullableLong(10, dto.deletedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                // 5. Attachment Metadata
                req.attachments.forEach { dto ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM attachments WHERE user_id = ? AND id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, dto.id)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > dto.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO attachments(user_id, id, consultation_id, question_id, target, kind, file_name, mime_type, size_bytes, duration_ms, caption, created_at, updated_at, deleted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id, id) DO UPDATE SET
                                caption = excluded.caption,
                                updated_at = excluded.updated_at,
                                deleted_at = excluded.deleted_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, dto.id)
                            ps.setString(3, dto.consultationId)
                            ps.setNullableString(4, dto.questionId)
                            ps.setString(5, dto.target)
                            ps.setString(6, dto.kind)
                            ps.setString(7, dto.fileName)
                            ps.setString(8, dto.mimeType)
                            ps.setLong(9, dto.sizeBytes)
                            ps.setNullableLong(10, dto.durationMs)
                            ps.setNullableString(11, dto.caption)
                            ps.setLong(12, dto.createdAtMillis)
                            ps.setLong(13, dto.updatedAtMillis)
                            ps.setNullableLong(14, dto.deletedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                // 6. Reminder Settings
                req.reminderSettings?.let { s ->
                    val existingUpdated = conn.prepareStatement(
                        "SELECT updated_at FROM user_settings WHERE user_id = ?"
                    ).use { ps ->
                        ps.setString(1, userId)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
                    }
                    if (existingUpdated != null && existingUpdated > s.updatedAtMillis) {
                        skipped++
                    } else {
                        conn.prepareStatement(
                            """
                            INSERT INTO user_settings(
                                user_id, visit_reminder_enabled, visit_reminder_hour, visit_30m_enabled,
                                visit_at_time_enabled, custom_lead_minutes, alarm_sound_mode_enabled,
                                prepare_reminder_enabled, followup_reminder_enabled, followup_reminder_hour,
                                backup_reminder_enabled, show_title_on_lock_screen, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(user_id) DO UPDATE SET
                                visit_reminder_enabled = excluded.visit_reminder_enabled,
                                visit_reminder_hour = excluded.visit_reminder_hour,
                                visit_30m_enabled = excluded.visit_30m_enabled,
                                visit_at_time_enabled = excluded.visit_at_time_enabled,
                                custom_lead_minutes = excluded.custom_lead_minutes,
                                alarm_sound_mode_enabled = excluded.alarm_sound_mode_enabled,
                                prepare_reminder_enabled = excluded.prepare_reminder_enabled,
                                followup_reminder_enabled = excluded.followup_reminder_enabled,
                                followup_reminder_hour = excluded.followup_reminder_hour,
                                backup_reminder_enabled = excluded.backup_reminder_enabled,
                                show_title_on_lock_screen = excluded.show_title_on_lock_screen,
                                updated_at = excluded.updated_at
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setInt(2, if (s.visitReminderEnabled) 1 else 0)
                            ps.setInt(3, s.visitReminderHour)
                            ps.setInt(4, if (s.visitThirtyMinBeforeEnabled) 1 else 0)
                            ps.setInt(5, if (s.visitAtTimeEnabled) 1 else 0)
                            ps.setNullableInt(6, s.customLeadMinutes)
                            ps.setInt(7, if (s.alarmSoundModeEnabled) 1 else 0)
                            ps.setInt(8, if (s.prepareReminderEnabled) 1 else 0)
                            ps.setInt(9, if (s.followUpReminderEnabled) 1 else 0)
                            ps.setInt(10, s.followUpReminderHour)
                            ps.setInt(11, if (s.backupReminderEnabled) 1 else 0)
                            ps.setInt(12, if (s.showTitleOnLockScreen) 1 else 0)
                            ps.setLong(13, s.updatedAtMillis)
                            ps.executeUpdate()
                        }
                        accepted++
                    }
                }

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }

        return SyncPushResponse(
            acceptedCount = accepted,
            skippedConflictsCount = skipped,
            serverTimestampMillis = serverNow
        )
    }

    fun pullSync(userId: String, sinceMillis: Long): SyncPullResponse {
        val serverNow = System.currentTimeMillis()
        return db.getConnection().use { conn ->
            val consultations = mutableListOf<SyncConsultationDto>()
            conn.prepareStatement(
                "SELECT * FROM consultations WHERE user_id = ? AND (updated_at >= ? OR (deleted_at IS NOT NULL AND deleted_at >= ?))"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.setLong(3, sinceMillis)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        consultations.add(
                            SyncConsultationDto(
                                id = rs.getString("id"),
                                title = rs.getString("title"),
                                appointmentAtMillis = rs.getLong("appointment_at"),
                                reason = rs.getString("reason"),
                                facilityName = rs.getString("facility_name"),
                                providerName = rs.getString("provider_name"),
                                status = rs.getString("status"),
                                createdAtMillis = rs.getLong("created_at"),
                                updatedAtMillis = rs.getLong("updated_at"),
                                deletedAtMillis = rs.getNullableLong("deleted_at")
                            )
                        )
                    }
                }
            }

            val questions = mutableListOf<SyncQuestionDto>()
            conn.prepareStatement(
                "SELECT * FROM questions WHERE user_id = ? AND (updated_at >= ? OR (deleted_at IS NOT NULL AND deleted_at >= ?))"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.setLong(3, sinceMillis)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        questions.add(
                            SyncQuestionDto(
                                id = rs.getString("id"),
                                consultationId = rs.getString("consultation_id"),
                                text = rs.getString("text"),
                                isPriority = rs.getInt("is_priority") == 1,
                                isDiscussed = rs.getInt("is_discussed") == 1,
                                createdAtMillis = rs.getLong("created_at"),
                                updatedAtMillis = rs.getLong("updated_at"),
                                answer = rs.getString("answer"),
                                answerUpdatedAtMillis = rs.getNullableLong("answer_updated_at"),
                                deletedAtMillis = rs.getNullableLong("deleted_at")
                            )
                        )
                    }
                }
            }

            val notes = mutableListOf<SyncNoteDto>()
            conn.prepareStatement(
                "SELECT * FROM consultation_notes WHERE user_id = ? AND (updated_at >= ? OR (deleted_at IS NOT NULL AND deleted_at >= ?))"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.setLong(3, sinceMillis)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        notes.add(
                            SyncNoteDto(
                                consultationId = rs.getString("consultation_id"),
                                summary = rs.getString("summary"),
                                providerInstruction = rs.getString("provider_instruction"),
                                additionalNotes = rs.getString("additional_notes"),
                                updatedAtMillis = rs.getLong("updated_at"),
                                deletedAtMillis = rs.getNullableLong("deleted_at")
                            )
                        )
                    }
                }
            }

            val followUps = mutableListOf<SyncFollowUpDto>()
            conn.prepareStatement(
                "SELECT * FROM follow_up_actions WHERE user_id = ? AND (updated_at >= ? OR (deleted_at IS NOT NULL AND deleted_at >= ?))"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.setLong(3, sinceMillis)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        followUps.add(
                            SyncFollowUpDto(
                                id = rs.getString("id"),
                                consultationId = rs.getString("consultation_id"),
                                text = rs.getString("text"),
                                dueDateEpochDay = rs.getNullableLong("due_date_epoch_day"),
                                isCompleted = rs.getInt("is_completed") == 1,
                                completedAtMillis = rs.getNullableLong("completed_at"),
                                createdAtMillis = rs.getLong("created_at"),
                                updatedAtMillis = rs.getLong("updated_at"),
                                deletedAtMillis = rs.getNullableLong("deleted_at")
                            )
                        )
                    }
                }
            }

            val attachments = mutableListOf<SyncAttachmentMetaDto>()
            conn.prepareStatement(
                "SELECT * FROM attachments WHERE user_id = ? AND (updated_at >= ? OR (deleted_at IS NOT NULL AND deleted_at >= ?))"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.setLong(3, sinceMillis)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        attachments.add(
                            SyncAttachmentMetaDto(
                                id = rs.getString("id"),
                                consultationId = rs.getString("consultation_id"),
                                questionId = rs.getString("question_id"),
                                target = rs.getString("target"),
                                kind = rs.getString("kind"),
                                fileName = rs.getString("file_name"),
                                mimeType = rs.getString("mime_type"),
                                sizeBytes = rs.getLong("size_bytes"),
                                durationMs = rs.getNullableLong("duration_ms"),
                                caption = rs.getString("caption"),
                                createdAtMillis = rs.getLong("created_at"),
                                updatedAtMillis = rs.getLong("updated_at"),
                                deletedAtMillis = rs.getNullableLong("deleted_at")
                            )
                        )
                    }
                }
            }

            val reminderSettings = conn.prepareStatement(
                "SELECT * FROM user_settings WHERE user_id = ? AND updated_at >= ?"
            ).use { ps ->
                ps.setString(1, userId)
                ps.setLong(2, sinceMillis)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        SyncReminderSettingsDto(
                            visitReminderEnabled = rs.getInt("visit_reminder_enabled") == 1,
                            visitReminderHour = rs.getInt("visit_reminder_hour"),
                            visitThirtyMinBeforeEnabled = rs.getInt("visit_30m_enabled") == 1,
                            visitAtTimeEnabled = rs.getInt("visit_at_time_enabled") == 1,
                            customLeadMinutes = rs.getObject("custom_lead_minutes")?.let { (it as Number).toInt() },
                            alarmSoundModeEnabled = rs.getInt("alarm_sound_mode_enabled") == 1,
                            prepareReminderEnabled = rs.getInt("prepare_reminder_enabled") == 1,
                            followUpReminderEnabled = rs.getInt("followup_reminder_enabled") == 1,
                            followUpReminderHour = rs.getInt("followup_reminder_hour"),
                            backupReminderEnabled = rs.getInt("backup_reminder_enabled") == 1,
                            showTitleOnLockScreen = rs.getInt("show_title_on_lock_screen") == 1,
                            updatedAtMillis = rs.getLong("updated_at")
                        )
                    } else null
                }
            }

            SyncPullResponse(
                serverTimestampMillis = serverNow,
                consultations = consultations,
                questions = questions,
                notes = notes,
                followUps = followUps,
                attachments = attachments,
                reminderSettings = reminderSettings
            )
        }
    }

    fun getUserAttachmentFile(userId: String, attachmentId: String): File {
        val safeId = attachmentId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(mediaDir, userId)
        dir.mkdirs()
        return File(dir, "$safeId.bin")
    }

    private fun insertRefreshToken(conn: Connection, userId: String, deviceName: String?, nowMs: Long): String {
        val rawToken = security.generateOpaqueToken()
        val tokenHash = security.sha256Hex(rawToken)
        conn.prepareStatement(
            "INSERT INTO refresh_tokens(token_hash, user_id, device_name, expires_at, created_at) VALUES (?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setString(1, tokenHash)
            ps.setString(2, userId)
            ps.setNullableString(3, deviceName)
            ps.setLong(4, nowMs + refreshTokenTtlMillis)
            ps.setLong(5, nowMs)
            ps.executeUpdate()
        }
        return rawToken
    }

    private fun java.sql.PreparedStatement.setNullableString(index: Int, value: String?) {
        if (value == null) setNull(index, Types.VARCHAR) else setString(index, value)
    }

    private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
        if (value == null) setNull(index, Types.BIGINT) else setLong(index, value)
    }

    private fun java.sql.PreparedStatement.setNullableInt(index: Int, value: Int?) {
        if (value == null) setNull(index, Types.INTEGER) else setInt(index, value)
    }

    private fun java.sql.ResultSet.getNullableLong(column: String): Long? {
        val value = getLong(column)
        return if (wasNull()) null else value
    }
}
