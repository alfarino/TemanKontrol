package id.temankontrol.app.data.sync

import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengoordinasikan pencadangan otomatis ke Supabase:
 * 1) Debounced (~3 detik) setiap kali pengguna menambah/mengubah data saat login.
 * 2) Penjadwalan ulang otomatis via JobScheduler saat perubahan gagal naik karena offline.
 * 3) Sinkronisasi ringan ketika aplikasi kembali ke foreground.
 */
@Singleton
class CloudAutoSyncCoordinator @Inject constructor(
    private val syncEngine: OfflineFirstSyncEngine,
    private val preferencesRepository: PreferencesRepository,
    private val syncScheduler: CloudSyncScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val lastForegroundCheckMillis = AtomicLong(0L)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return

        // 1. Kelola jadwal periodik latar belakang berdasarkan status login akun
        scope.launch {
            preferencesRepository.accountSession
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        syncScheduler.schedulePeriodicBackup()
                        if (preferencesRepository.hasUnsyncedCloudChanges.first()) {
                            runAutoSyncNow("session_restored_pending")
                        }
                    } else {
                        syncScheduler.cancelAll()
                    }
                }
        }

        // 2. Debounce mutasi data lokal (konsultasi, pertanyaan, catatan, tindak lanjut, foto/VN, pengingat)
        scope.launch {
            preferencesRepository.lastDataChangeMillis
                .debounce(3_000L)
                .collect { changeTs ->
                    if (changeTs <= 0L) return@collect
                    val session = preferencesRepository.accountSession.first() ?: return@collect
                    if (!preferencesRepository.hasUnsyncedCloudChanges.first()) return@collect
                    AppLogger.d("CloudAutoSync", "Detected local change ($changeTs) for ${session.userId}, syncing to Supabase…")
                    runAutoSyncNow("debounced_mutation")
                }
        }
    }

    fun onAppForeground() {
        val now = System.currentTimeMillis()
        val prev = lastForegroundCheckMillis.get()
        if (now - prev < 60_000L) return
        lastForegroundCheckMillis.set(now)

        scope.launch {
            val result = syncEngine.syncIfPendingOrStale(maxStaleMillis = 5 * 60_000L)
            if (result is AppResult.Failure && preferencesRepository.hasUnsyncedCloudChanges.first()) {
                syncScheduler.scheduleImmediatePendingRetry()
            }
        }
    }

    private suspend fun runAutoSyncNow(reason: String) {
        val result = syncEngine.syncNow(forceFullRestore = false)
        when (result) {
            is AppResult.Success -> AppLogger.d("CloudAutoSync", "Auto-sync ($reason) ok: pushed=${result.data.pushedRecords}")
            is AppResult.Failure -> {
                AppLogger.d("CloudAutoSync", "Auto-sync ($reason) deferred (likely offline), scheduling JobScheduler retry")
                syncScheduler.scheduleImmediatePendingRetry()
            }
        }
    }
}
