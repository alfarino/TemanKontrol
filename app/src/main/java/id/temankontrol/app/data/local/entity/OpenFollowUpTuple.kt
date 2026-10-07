package id.temankontrol.app.data.local.entity

import androidx.room.Embedded

data class OpenFollowUpTuple(
    @Embedded
    val action: FollowUpActionEntity,

    val consultationTitle: String
)
