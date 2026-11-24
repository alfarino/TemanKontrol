package id.temankontrol.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To3_preservesExistingDataAndAddsV3Tables() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                "INSERT INTO consultations (id, title, appointment_at, reason, facility_name, provider_name, status, created_at, updated_at) " +
                    "VALUES ('c-v1', 'Kontrol Lama', 1775500000000, 'Cek tensi', 'RS Lama', 'dr. Budi', 'SCHEDULED', 1775000000000, 1775000000000)"
            )
            execSQL(
                "INSERT INTO questions (id, consultation_id, text, text_normalized, is_priority, is_discussed, created_at, updated_at) " +
                    "VALUES ('q-v1', 'c-v1', 'Bagaimana hasil tensi?', 'bagaimana hasil tensi?', 1, 0, 1775000000000, 1775000000000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3
        )

        val cursor = db.query("SELECT text, answer FROM questions WHERE id = 'q-v1'")
        assertEquals(true, cursor.moveToFirst())
        assertEquals("Bagaimana hasil tensi?", cursor.getString(0))
        assertNull(cursor.getString(1))
        cursor.close()
    }
}
