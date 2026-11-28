package id.temankontrol.app

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import id.temankontrol.app.data.local.dao.AttachmentDao
import id.temankontrol.app.data.media.MediaAttachmentManager
import id.temankontrol.app.data.reminder.ReminderNotificationHelper
import id.temankontrol.app.data.reminder.ReminderScheduler
import id.temankontrol.app.data.sync.CloudAutoSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TemanKontrolApp : Application() {

    @Inject
    lateinit var attachmentDao: AttachmentDao

    @Inject
    lateinit var mediaAttachmentManager: MediaAttachmentManager

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var cloudAutoSyncCoordinator: CloudAutoSyncCoordinator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        ReminderNotificationHelper.ensureChannels(this)
        cloudAutoSyncCoordinator.start()
        appScope.launch {
            mediaAttachmentManager.cleanupOrphanFiles(attachmentDao)
            reminderScheduler.rescheduleAll()
        }
    }
}
