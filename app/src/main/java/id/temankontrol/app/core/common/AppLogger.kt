package id.temankontrol.app.core.common

import android.util.Log
import id.temankontrol.app.BuildConfig

/**
 * Logger aman sesuai NFR-017 & §11.2 butir 13:
 * Hanya mencatat tag teknis & ID/kategori error pada build debug,
 * tidak pernah mencatat isi teks pengguna atau data sensitif.
 */
object AppLogger {
    private const val DEFAULT_TAG = "TemanKontrol"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }
}
