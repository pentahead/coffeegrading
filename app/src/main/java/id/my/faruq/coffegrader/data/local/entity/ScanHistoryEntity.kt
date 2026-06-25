package id.my.faruq.coffegrader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nama batch / kode sampel
    val batchName: String,

    // Waktu scan
    val dateTime: String,

    // Total biji terdeteksi
    val totalBeans: Int,

    // Total nilai cacat (desimal sesuai SNI)
    val defectScore: Double,

    // Mutu hasil grading (1,2,3,4a,...)
    val gradeText: String,

    // Path gambar asli (untuk ML & preview di detail)
    val imagePath: String? = null,

    // Path thumbnail (untuk list & carousel)
    val thumbnailPath: String? = null,

    // ID sampel (info SNI) yang diisi sebelum scan
    val sampleInfoId: Long? = null,

    // Durasi proses scan (ms) - untuk ditampilkan di detail riwayat
    val scanDurationMs: Long = 0L,

    // Rata-rata confidence score seluruh deteksi (0.0–1.0)
    val confidenceScoreTotal: Double = 0.0,
)
