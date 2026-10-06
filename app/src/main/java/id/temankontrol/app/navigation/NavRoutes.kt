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
data class ConsultationDetailRoute(val id: String)

@Serializable
data class ConsultationFormRoute(val id: String? = null)

@Serializable
data class QuestionRoute(val consultationId: String)

@Serializable
data class NoteRoute(val consultationId: String)

@Serializable
data class FollowUpRoute(val consultationId: String)
