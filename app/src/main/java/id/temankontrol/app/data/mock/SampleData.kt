package id.temankontrol.app.data.mock

import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationNote
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.Question
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SampleData {
    val baseTime: Instant get() = Instant.now().truncatedTo(ChronoUnit.HOURS)
    val baseDate: LocalDate get() = LocalDate.now()

    fun getSampleConsultations(): List<Consultation> = listOf(
        Consultation(
            id = ConsultationId("c-001"),
            title = "Kontrol Hipertensi Rutin",
            appointmentAt = baseTime.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
            reason = "Pemeriksaan tensi bulanan dan evaluasi obat",
            facilityName = "RS Sehat Bersama",
            providerName = "dr. Budi Santoso, Sp.PD",
            status = ConsultationStatus.SCHEDULED,
            createdAt = baseTime.minus(3, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(1, ChronoUnit.DAYS)
        ),
        Consultation(
            id = ConsultationId("c-002"),
            title = "Konsultasi Mata Berkunang & Kabur",
            appointmentAt = baseTime.plus(5, ChronoUnit.DAYS),
            reason = "Mata sering kabur saat melihat jauh",
            facilityName = "Klinik Spesialis Mata Indah",
            providerName = "dr. Siti Rahma, Sp.M",
            status = ConsultationStatus.SCHEDULED,
            createdAt = baseTime.minus(2, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(2, ChronoUnit.DAYS)
        ),
        Consultation(
            id = ConsultationId("c-003"),
            title = "Pemeriksaan Gigi dan Karang Gigi",
            appointmentAt = baseTime.minus(2, ChronoUnit.DAYS),
            reason = "Pembersihan karang gigi berkala",
            facilityName = "Puskesmas Melati",
            providerName = "drg. Ahmad Fauzi",
            status = ConsultationStatus.SCHEDULED,
            createdAt = baseTime.minus(7, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(7, ChronoUnit.DAYS)
        ),
        Consultation(
            id = ConsultationId("c-004"),
            title = "Konsultasi Jantung Pasca Rawat Inap",
            appointmentAt = baseTime.minus(10, ChronoUnit.DAYS),
            reason = "Kontrol pasca pemasangan ring jantung",
            facilityName = "RS Jantung Harapan Kita",
            providerName = "dr. Hendra Prasetya, Sp.JP",
            status = ConsultationStatus.COMPLETED,
            createdAt = baseTime.minus(15, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Consultation(
            id = ConsultationId("c-005"),
            title = "Konsultasi Alergi Makanan & Gatal Kulit",
            appointmentAt = baseTime.minus(20, ChronoUnit.DAYS),
            reason = "Ruam gatal di lengan dan leher",
            facilityName = "RS Sehat Bersama",
            providerName = "dr. Maya Lestari, Sp.KK",
            status = ConsultationStatus.COMPLETED,
            createdAt = baseTime.minus(25, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(20, ChronoUnit.DAYS)
        ),
        Consultation(
            id = ConsultationId("c-006"),
            title = "Konsultasi Batuk Kronis",
            appointmentAt = baseTime.minus(5, ChronoUnit.DAYS),
            reason = "Batuk tidak kunjung sembuh lebih dari 2 minggu",
            facilityName = "Klinik Paru Sejahtera",
            providerName = "dr. Rian Pratama, Sp.P",
            status = ConsultationStatus.CANCELLED,
            createdAt = baseTime.minus(12, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(5, ChronoUnit.DAYS)
        )
    )

    fun getSampleQuestions(): List<Question> = listOf(
        Question(
            id = "q-101",
            consultationId = ConsultationId("c-001"),
            text = "Apakah dosis obat tensi saat ini masih sesuai?",
            isPriority = true,
            isDiscussed = false,
            createdAt = baseTime.minus(3, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(3, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-102",
            consultationId = ConsultationId("c-001"),
            text = "Keluhan pusing saat bangun tidur apakah efek samping obat?",
            isPriority = true,
            isDiscussed = false,
            createdAt = baseTime.minus(2, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(2, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-103",
            consultationId = ConsultationId("c-001"),
            text = "Apakah perlu pemeriksaan rekam jantung atau lab darah lagi?",
            isPriority = false,
            isDiscussed = false,
            createdAt = baseTime.minus(1, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(1, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-201",
            consultationId = ConsultationId("c-002"),
            text = "Apakah penurunan penglihatan ini tanda awal katarak?",
            isPriority = true,
            isDiscussed = false,
            createdAt = baseTime.minus(2, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(2, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-202",
            consultationId = ConsultationId("c-002"),
            text = "Bolehkah memakai obat tetes mata yang dijual bebas?",
            isPriority = false,
            isDiscussed = false,
            createdAt = baseTime.minus(2, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(2, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-301",
            consultationId = ConsultationId("c-003"),
            text = "Kapan waktu yang tepat untuk pembersihan karang gigi berikutnya?",
            isPriority = false,
            isDiscussed = false,
            createdAt = baseTime.minus(7, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(7, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-401",
            consultationId = ConsultationId("c-004"),
            text = "Bagaimana cara membedakan nyeri dada yang masih wajar dan yang harus segera diperiksa?",
            isPriority = true,
            isDiscussed = true,
            createdAt = baseTime.minus(15, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS),
            answer = "Nyeri ringan sebentar saat berubah posisi masih wajar, namun bila terasa menekan > 10 menit atau menjalar ke lengan/rahang harus segera ke IGD.",
            answerUpdatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-402",
            consultationId = ConsultationId("c-004"),
            text = "Aktivitas fisik apa saja yang sudah boleh saya lakukan?",
            isPriority = true,
            isDiscussed = true,
            createdAt = baseTime.minus(14, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-403",
            consultationId = ConsultationId("c-004"),
            text = "Kapan jadwal pemeriksaan echocardiografi ulang?",
            isPriority = false,
            isDiscussed = true,
            createdAt = baseTime.minus(12, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-501",
            consultationId = ConsultationId("c-005"),
            text = "Apakah tes alergi diperlukan?",
            isPriority = false,
            isDiscussed = true,
            createdAt = baseTime.minus(25, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(20, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-502",
            consultationId = ConsultationId("c-005"),
            text = "Berapa lama salep kortikosteroid aman digunakan?",
            isPriority = false,
            isDiscussed = true,
            createdAt = baseTime.minus(25, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(20, ChronoUnit.DAYS)
        ),
        Question(
            id = "q-601",
            consultationId = ConsultationId("c-006"),
            text = "Apakah batuk malam hari disebabkan asam lambung?",
            isPriority = false,
            isDiscussed = false,
            createdAt = baseTime.minus(12, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(12, ChronoUnit.DAYS)
        )
    )

    fun getSampleNotes(): List<ConsultationNote> = listOf(
        ConsultationNote(
            consultationId = ConsultationId("c-004"),
            summary = "Penyakit jantung koroner stabil pasca pemasangan ring 1 bulan lalu",
            providerInstruction = "Hindari mengangkat beban berat > 5kg, jalan santai 20 menit tiap pagi",
            additionalNotes = "Lanjut obat rutin dan bawa hasil rekam jantung pada kontrol berikutnya",
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        ConsultationNote(
            consultationId = ConsultationId("c-005"),
            summary = "Dermatitis kontak alergika akibat makanan laut",
            providerInstruction = "Hindari seafood dan gunakan pelembap secara teratur",
            additionalNotes = null,
            updatedAt = baseTime.minus(20, ChronoUnit.DAYS)
        )
    )

    fun getSampleFollowUps(): List<FollowUpAction> = listOf(
        FollowUpAction(
            id = "f-101",
            consultationId = ConsultationId("c-001"),
            text = "Catat tekanan darah mandiri setiap pagi",
            dueDate = baseDate.plusDays(7),
            isCompleted = false,
            completedAt = null,
            createdAt = baseTime.minus(3, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(3, ChronoUnit.DAYS)
        ),
        FollowUpAction(
            id = "f-301",
            consultationId = ConsultationId("c-003"),
            text = "Jadwalkan ulang kontrol pembersihan karang gigi",
            dueDate = null,
            isCompleted = false,
            completedAt = null,
            createdAt = baseTime.minus(7, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(7, ChronoUnit.DAYS)
        ),
        FollowUpAction(
            id = "f-401",
            consultationId = ConsultationId("c-004"),
            text = "Tebus resep obat rutin bulan kedua",
            dueDate = baseDate.minusDays(8),
            isCompleted = true,
            completedAt = baseTime.minus(8, ChronoUnit.DAYS),
            createdAt = baseTime.minus(10, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(8, ChronoUnit.DAYS)
        ),
        FollowUpAction(
            id = "f-402",
            consultationId = ConsultationId("c-004"),
            text = "Buat jadwal pemeriksaan berkala bulan depan",
            dueDate = baseDate.plusDays(14),
            isCompleted = false,
            completedAt = null,
            createdAt = baseTime.minus(10, ChronoUnit.DAYS),
            updatedAt = baseTime.minus(10, ChronoUnit.DAYS)
        )
    )

    fun getSampleAttachments(): List<Attachment> = listOf(
        Attachment(
            id = "att-101",
            consultationId = ConsultationId("c-001"),
            questionId = "q-101",
            target = AttachmentTarget.QUESTION,
            kind = AttachmentKind.PHOTO,
            fileName = "hasil_tensi_mingguan.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 285_400L,
            caption = "Catatan tensi pagi 7 hari terakhir",
            createdAt = baseTime.minus(1, ChronoUnit.DAYS)
        ),
        Attachment(
            id = "att-401",
            consultationId = ConsultationId("c-004"),
            questionId = "q-401",
            target = AttachmentTarget.QUESTION,
            kind = AttachmentKind.VOICE,
            fileName = "penjelasan_nyeri_dada.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 320_000L,
            durationMs = 82_000L,
            caption = "Penjelasan dokter tentang tanda bahaya",
            createdAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Attachment(
            id = "att-402",
            consultationId = ConsultationId("c-004"),
            questionId = null,
            target = AttachmentTarget.ADDITIONAL_NOTE,
            kind = AttachmentKind.PHOTO,
            fileName = "hasil_ekg_oktober.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 412_800L,
            caption = "Foto lembaran hasil EKG kontrol",
            createdAt = baseTime.minus(10, ChronoUnit.DAYS)
        ),
        Attachment(
            id = "att-403",
            consultationId = ConsultationId("c-004"),
            questionId = null,
            target = AttachmentTarget.ADDITIONAL_NOTE,
            kind = AttachmentKind.VOICE,
            fileName = "arahan_olahraga_tambahan.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 195_000L,
            durationMs = 48_000L,
            caption = "Arahan tambahan jadwal jalan pagi",
            createdAt = baseTime.minus(10, ChronoUnit.DAYS)
        )
    )
}
