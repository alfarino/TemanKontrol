package id.temankontrol.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `questions` ADD COLUMN `answer` TEXT")
            db.execSQL("ALTER TABLE `questions` ADD COLUMN `answer_updated_at` INTEGER")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reminder_log` (" +
                    "`key` TEXT NOT NULL, " +
                    "`fired_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`key`)" +
                    ")"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `attachments` (" +
                    "`id` TEXT NOT NULL, " +
                    "`consultation_id` TEXT NOT NULL, " +
                    "`question_id` TEXT, " +
                    "`target` TEXT NOT NULL, " +
                    "`kind` TEXT NOT NULL, " +
                    "`file_name` TEXT NOT NULL, " +
                    "`mime_type` TEXT NOT NULL, " +
                    "`size_bytes` INTEGER NOT NULL, " +
                    "`duration_ms` INTEGER, " +
                    "`caption` TEXT, " +
                    "`uri_string` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`consultation_id`) REFERENCES `consultations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`question_id`) REFERENCES `questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_attachments_consultation_id_kind` " +
                    "ON `attachments` (`consultation_id`, `kind`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_attachments_question_id` " +
                    "ON `attachments` (`question_id`)"
            )
        }
    }
}
