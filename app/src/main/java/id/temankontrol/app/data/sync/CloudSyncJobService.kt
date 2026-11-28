package id.temankontrol.app.data.sync

import android.app.job.JobParameters
import android.app.job.JobService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudSyncJobEntryPoint {
    fun syncEngine(): OfflineFirstSyncEngine
    fun preferencesRepository(): PreferencesRepository
}

class CloudSyncJobService : JobService() {

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CloudSyncJobEntryPoint::class.java
        )
        val syncEngine = entryPoint.syncEngine()
        val prefs = entryPoint.preferencesRepository()

        jobScope.launch {
            var needsReschedule = false
            try {
                val session = prefs.accountSession.first()
                if (session != null) {
                    val result = if (params.jobId == CloudSyncScheduler.JOB_ID_IMMEDIATE_RETRY) {
                        val hasPending = prefs.hasUnsyncedCloudChanges.first()
                        if (hasPending) syncEngine.syncNow(forceFullRestore = false) else null
                    } else {
                        syncEngine.syncNow(forceFullRestore = false)
                    }
                    if (result is AppResult.Failure &&
                        params.jobId == CloudSyncScheduler.JOB_ID_IMMEDIATE_RETRY
                    ) {
                        needsReschedule = true
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("CloudSyncJob", "Background sync failed", e)
                if (params.jobId == CloudSyncScheduler.JOB_ID_IMMEDIATE_RETRY) {
                    needsReschedule = true
                }
            } finally {
                jobFinished(params, needsReschedule)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        jobScope.cancel()
        return true
    }
}
