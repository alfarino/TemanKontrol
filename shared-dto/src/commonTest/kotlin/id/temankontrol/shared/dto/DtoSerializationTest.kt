package id.temankontrol.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoSerializationTest {

    @Test
    fun syncPushRequest_roundTripSerialization() {
        val payload = SyncPushRequest(
            consultations = listOf(
                SyncConsultationDto(
                    id = "c-1",
                    title = "Kontrol Hipertensi",
                    appointmentAtMillis = 1795600000000L,
                    reason = "Evaluasi tekanan darah",
                    facilityName = "RS Unand",
                    providerName = "dr. Budi",
                    status = "SCHEDULED",
                    createdAtMillis = 1795500000000L,
                    updatedAtMillis = 1795500000000L
                )
            )
        )
        val encoded = Json.encodeToString(payload)
        val decoded = Json.decodeFromString<SyncPushRequest>(encoded)
        assertEquals(payload, decoded)
    }
}
