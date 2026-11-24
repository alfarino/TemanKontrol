package id.temankontrol.app.data.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupCryptoTest {

    private fun createSampleZip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(text.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return out.toByteArray()
    }

    @Test
    fun plainPackAndUnpack_roundTripsZipPayload() {
        val sampleJson = """{"consultations":[{"id":"c-1","title":"Tes"}]}"""
        val zip = createSampleZip(sampleJson)

        val container = BackupCrypto.packContainer(zip, password = null)
        assertFalse(BackupCrypto.isContainerEncrypted(container))

        val unpacked = BackupCrypto.unpackContainer(container, password = null)
        assertArrayEquals(zip, unpacked)

        val zis = ZipInputStream(unpacked.inputStream())
        val entry = zis.nextEntry
        assertEquals("data.json", entry?.name)
        assertEquals(sampleJson, zis.readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun encryptedPackAndUnpack_roundTripsWithCorrectPassword() {
        val sampleJson = """{"secret":"riwayat kesehatan"}"""
        val zip = createSampleZip(sampleJson)

        val container = BackupCrypto.packContainer(zip, password = "SandiKuat123!")
        assertTrue(BackupCrypto.isContainerEncrypted(container))

        // Ciphertext must not contain plaintext JSON directly
        val containerText = String(container, Charsets.ISO_8859_1)
        assertFalse(containerText.contains("riwayat kesehatan"))

        val unpacked = BackupCrypto.unpackContainer(container, password = "SandiKuat123!")
        assertArrayEquals(zip, unpacked)
    }

    @Test(expected = SecurityException::class)
    fun encryptedUnpack_wrongPassword_throwsSecurityException() {
        val zip = createSampleZip("""{"a":1}""")
        val container = BackupCrypto.packContainer(zip, password = "BenarPassword1")
        BackupCrypto.unpackContainer(container, password = "SalahPassword2")
    }

    @Test(expected = IllegalArgumentException::class)
    fun unpack_corruptMagic_throwsIllegalArgument() {
        BackupCrypto.unpackContainer("NOT_TKBK_FORMAT".toByteArray(), null)
    }

    @Test
    fun sha256Hex_isDeterministicAnd64Chars() {
        val h1 = BackupCrypto.sha256Hex("TemanKontrol".toByteArray())
        val h2 = BackupCrypto.sha256Hex("TemanKontrol".toByteArray())
        val h3 = BackupCrypto.sha256Hex("TemanKontrol2".toByteArray())
        assertEquals(64, h1.length)
        assertEquals(h1, h2)
        assertNotEquals(h1, h3)
    }
}
