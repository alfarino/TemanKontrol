package id.temankontrol.app.navigation

import kotlinx.serialization.Serializable

@Serializable
object NoticeRoute

@Serializable
object HomeRoute

@Serializable
object HistoryRoute

@Serializable
object AboutRoute

@Serializable
object SettingsRoute

@Serializable
object BackupRoute

@Serializable
object ReminderSettingsRoute

@Serializable
data class ConsultationDetailRoute(val id: String)

@Serializable
data class ConsultationFormRoute(val id: String? = null)

@Serializable
data class QuestionRoute(val consultationId: String)

@Serializable
data class NoteRoute(val consultationId: String)

@Serializable
data class FollowUpRoute(val consultationId: String)

@Serializable
data class ConsultationModeRoute(val consultationId: String)

@Serializable
data class SummaryPreviewRoute(val consultationId: String)
