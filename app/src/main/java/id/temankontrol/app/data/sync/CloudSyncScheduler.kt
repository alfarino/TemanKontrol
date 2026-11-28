package id.temankontrol.app.data.sync

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.temankontrol.app.core.common.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val JOB_ID_PERIODIC = 9101
        const val JOB_ID_IMMEDIATE_RETRY = 9102
        private const val PERIODIC_INTERVAL_MS = 6L * 60L * 60L * 1000L // setiap 6 jam
    }

    private fun jobScheduler(): JobScheduler? =
        context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler

    fun schedulePeriodicBackup() {
        val scheduler = jobScheduler() ?: return
        if (scheduler.getPendingJob(JOB_ID_PERIODIC) != null) return

        val component = ComponentName(context, CloudSyncJobService::class.java)
        val job = JobInfo.Builder(JOB_ID_PERIODIC, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIODIC_INTERVAL_MS)
            .setPersisted(true)
            .build()
        val result = scheduler.schedule(job)
        AppLogger.d("CloudSync", "Scheduled periodic backup job result=$result")
    }

    fun scheduleImmediatePendingRetry() {
        val scheduler = jobScheduler() ?: return
        val component = ComponentName(context, CloudSyncJobService::class.java)
        val job = JobInfo.Builder(JOB_ID_IMMEDIATE_RETRY, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(15_000L)
            .setBackoffCriteria(60_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build()
        val result = scheduler.schedule(job)
        AppLogger.d("CloudSync", "Scheduled pending network retry job result=$result")
    }

    fun cancelAll() {
        val scheduler = jobScheduler() ?: return
        scheduler.cancel(JOB_ID_PERIODIC)
        scheduler.cancel(JOB_ID_IMMEDIATE_RETRY)
    }
}
