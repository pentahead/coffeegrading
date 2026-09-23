package id.my.faruq.coffegrader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grading_results")
data class GradingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val totalDefect: Double,
    val mutu: String,
    val detectedClasses: String,
    val timestamp: Long,
)
