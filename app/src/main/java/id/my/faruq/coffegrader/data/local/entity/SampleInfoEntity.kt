package id.my.faruq.coffegrader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sample_info")
data class SampleInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val batchId: String,
    val origin: String? = null,

    val coffeeType: String,        // Robusta / Arabika
    val processingMethod: String,  // Dry / Wet
    val beanSize: String,          // Besar/Sedang/Kecil
    val beanShape: String,         // Normal/Peaberry/Polyembrio

    val hasInsect: Boolean,
    val hasMoldSmell: Boolean,

    val moistureContent: Float,    // max 12.5
    val dirtContent: Float,        // max 0.5

    val sortationType: String,     // Primer/Sekunder/Asalan

    val timestamp: Long = System.currentTimeMillis()
)
