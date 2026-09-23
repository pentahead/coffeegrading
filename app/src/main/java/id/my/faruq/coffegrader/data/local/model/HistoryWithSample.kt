package id.my.faruq.coffegrader.data.local.model

data class HistoryWithSample(
    val historyId: Long,
    val batchId: String,
    val gradeResult: String,
    val totalDefectScore: Float,
    val timestamp: Long
)
