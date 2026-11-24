package id.temankontrol.app.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Format kontainer `.tkbackup` (NFR-041 & NFR-042):
 * - Header: "TKBK" (4 byte) | version (Short = 1) | flag (Byte: 0x00 = plain ZIP, 0x01 = AES-256-GCM)
 * - Jika terenkripsi: salt (16 byte) | iv (12 byte) | ciphertext (GCM 128-bit tag)
 * - Jika plain: raw ZIP payload
 */
object BackupCrypto {

    private val MAGIC = byteArrayOf('T'.code.toByte(), 'K'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    const val CONTAINER_VERSION: Short = 1
    const val FLAG_PLAIN: Byte = 0x00
    const val FLAG_ENCRYPTED: Byte = 0x01
    const val PBKDF2_ITERATIONS = 600_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256

    private val secureRandom = SecureRandom()

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun packContainer(zipPayload: ByteArray, password: String?): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.write(MAGIC)
            dos.writeShort(CONTAINER_VERSION.toInt())
            if (password.isNullOrEmpty()) {
                dos.writeByte(FLAG_PLAIN.toInt())
                dos.write(zipPayload)
            } else {
                dos.writeByte(FLAG_ENCRYPTED.toInt())
                val salt = ByteArray(SALT_LEN).also { secureRandom.nextBytes(it) }
                val iv = ByteArray(IV_LEN).also { secureRandom.nextBytes(it) }
                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                val cipherText = cipher.doFinal(zipPayload)
                dos.write(salt)
                dos.write(iv)
                dos.write(cipherText)
            }
        }
        return out.toByteArray()
    }

    fun isContainerEncrypted(containerBytes: ByteArray): Boolean {
        if (containerBytes.size < 7) throw IllegalArgumentException("Berkas cadangan tidak valid.")
        val dis = DataInputStream(ByteArrayInputStream(containerBytes))
        val magic = ByteArray(4)
        dis.readFully(magic)
        if (!magic.contentEquals(MAGIC)) throw IllegalArgumentException("Format berkas bukan .tkbackup.")
        val version = dis.readShort()
        if (version > CONTAINER_VERSION) throw UnsupportedOperationException("Versi cadangan lebih baru.")
        val flag = dis.readByte()
        return flag == FLAG_ENCRYPTED
    }

    fun unpackContainer(containerBytes: ByteArray, password: String?): ByteArray {
        if (containerBytes.size < 7) throw IllegalArgumentException("Berkas cadangan rusak.")
        val dis = DataInputStream(ByteArrayInputStream(containerBytes))
        val magic = ByteArray(4)
        dis.readFully(magic)
        if (!magic.contentEquals(MAGIC)) throw IllegalArgumentException("Format berkas bukan .tkbackup.")
        val version = dis.readShort()
        if (version > CONTAINER_VERSION) throw UnsupportedOperationException("Versi cadangan lebih baru.")
        val flag = dis.readByte()

        return when (flag) {
            FLAG_PLAIN -> dis.readBytes()
            FLAG_ENCRYPTED -> {
                if (password.isNullOrEmpty()) throw SecurityException("Kata sandi diperlukan.")
                val salt = ByteArray(SALT_LEN)
                val iv = ByteArray(IV_LEN)
                dis.readFully(salt)
                dis.readFully(iv)
                val cipherText = dis.readBytes()
                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                try {
                    cipher.doFinal(cipherText)
                } catch (e: Exception) {
                    throw SecurityException("Kata sandi salah atau berkas rusak.", e)
                }
            }
            else -> throw IllegalArgumentException("Flag kontainer tidak dikenal.")
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }
}
