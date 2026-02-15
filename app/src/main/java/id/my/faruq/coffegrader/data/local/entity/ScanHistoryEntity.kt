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

    // Thumbnail path optional
    val thumbnailPath: String? = null,

    // ID sampel (info SNI) yang diisi sebelum scan
    val sampleInfoId: Long? = null
)
