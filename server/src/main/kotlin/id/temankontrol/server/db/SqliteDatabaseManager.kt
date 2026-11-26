package id.temankontrol.server.db

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SqliteDatabaseManager(
    private val dbPath: String
) {
    val journalMode: String

    init {
        if (dbPath != ":memory:") {
            File(dbPath).parentFile?.mkdirs()
        }
        journalMode = initializeSchema()
    }

    fun getConnection(): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        conn.createStatement().use { st ->
            st.execute("PRAGMA foreign_keys = ON;")
            st.execute("PRAGMA busy_timeout = 5000;")
        }
        return conn
    }

    private fun initializeSchema(): String {
        var mode = "wal"
        getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA journal_mode = WAL;").use { rs ->
                    if (rs.next()) {
                        mode = rs.getString(1)
                    }
                }
                st.execute("PRAGMA synchronous = NORMAL;")
                st.execute("PRAGMA foreign_keys = ON;")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        email TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS refresh_tokens (
                        token_hash TEXT PRIMARY KEY,
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        device_name TEXT,
                        expires_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS otp_codes (
                        email TEXT PRIMARY KEY,
                        otp_hash TEXT NOT NULL,
                        expires_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS consultations (
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        appointment_at INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        facility_name TEXT,
                        provider_name TEXT,
                        status TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY (user_id, id)
                    )
                    """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_consultations_user_updated ON consultations(user_id, updated_at)")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS questions (
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        id TEXT NOT NULL,
                        consultation_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        is_priority INTEGER NOT NULL,
                        is_discussed INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        answer TEXT,
                        answer_updated_at INTEGER,
                        deleted_at INTEGER,
                        PRIMARY KEY (user_id, id)
                    )
                    """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_questions_user_updated ON questions(user_id, updated_at)")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS consultation_notes (
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        consultation_id TEXT NOT NULL,
                        summary TEXT,
                        provider_instruction TEXT,
                        additional_notes TEXT,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY (user_id, consultation_id)
                    )
                    """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_updated ON consultation_notes(user_id, updated_at)")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS follow_up_actions (
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        id TEXT NOT NULL,
                        consultation_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        due_date_epoch_day INTEGER,
                        is_completed INTEGER NOT NULL,
                        completed_at INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY (user_id, id)
                    )
                    """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_followups_user_updated ON follow_up_actions(user_id, updated_at)")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS attachments (
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        id TEXT NOT NULL,
                        consultation_id TEXT NOT NULL,
                        question_id TEXT,
                        target TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        file_name TEXT NOT NULL,
                        mime_type TEXT NOT NULL,
                        size_bytes INTEGER NOT NULL,
                        duration_ms INTEGER,
                        caption TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        PRIMARY KEY (user_id, id)
                    )
                    """.trimIndent()
                )
                st.execute("CREATE INDEX IF NOT EXISTS idx_attachments_user_updated ON attachments(user_id, updated_at)")

                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS user_settings (
                        user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                        visit_reminder_enabled INTEGER NOT NULL,
                        visit_reminder_hour INTEGER NOT NULL,
                        visit_30m_enabled INTEGER NOT NULL,
                        visit_at_time_enabled INTEGER NOT NULL,
                        custom_lead_minutes INTEGER,
                        alarm_sound_mode_enabled INTEGER NOT NULL,
                        prepare_reminder_enabled INTEGER NOT NULL,
                        followup_reminder_enabled INTEGER NOT NULL,
                        followup_reminder_hour INTEGER NOT NULL,
                        backup_reminder_enabled INTEGER NOT NULL,
                        show_title_on_lock_screen INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        return mode
    }
}
