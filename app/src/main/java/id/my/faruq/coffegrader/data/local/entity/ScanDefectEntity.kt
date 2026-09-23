package id.my.faruq.coffegrader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_defect")
data class ScanDefectEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // FK ke ScanHistory
    val historyId: Long,

    val defectName: String,
    val defectCount: Int,
    val defectValue: Double
)
