package id.my.faruq.coffegrader.data.repository

import androidx.compose.ui.graphics.Color
import id.my.faruq.coffegrader.data.local.dao.ScanDefectDao
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** UI model untuk halaman detail riwayat */
data class HistoryDetailUi(
    val id: String,
    val batchName: String,
    val timeText: String,
    val dateText: String,
    val gradeText: String,
    val themeColor: Color,
    val totalBeans: Int,
    val defectiveBeans: Int,
    val defectScoreTotal: Float,
    val dominantDefect: String,
    val scanDurationMs: Long,
    val officerName: String,
    val defects: List<DefectRowUi>
)

/** Satu baris cacat di tabel detail */
data class DefectRowUi(
    val no: Int,
    val defectName: String,
    val defectScore: Float,
    val count: Int,
    val totalScore: Float
)

class HistoryRepository @Inject constructor(
    private val historyDao: ScanHistoryDao,
    private val defectDao: ScanDefectDao
) {

    fun getAllHistory() = historyDao.getAllHistory()

    suspend fun seedDummyIfEmpty() {
        val count = historyDao.countHistory()

        if (count == 0) {
            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_001",
                    dateTime = "08:15 / 2025-11-30",
                    totalBeans = 132,
                    defectScore = 11,
                    gradeText = "1"
                )
            )

            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_002",
                    dateTime = "08:16 / 2025-11-30",
                    totalBeans = 132,
                    defectScore = 33,
                    gradeText = "4a"
                )
            )

            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_003",
                    dateTime = "08:18 / 2025-11-30",
                    totalBeans = 128,
                    defectScore = 92,
                    gradeText = "5"
                )
            )
        }
    }

    // ===== DETAIL UTAMA =====
    fun getDetail(historyId: Long): Flow<HistoryDetailUi?> {
        return historyDao.getHistoryDetail(historyId).map { entity ->
            entity?.let {
                HistoryDetailUi(
                    id = it.id.toString(),
                    batchName = it.batchName,
                    timeText = it.dateTime.split("/")[0].trim(),
                    dateText = it.dateTime.split("/")[1].trim(),
                    gradeText = it.gradeText,
                    themeColor = Color(0xFF12A150),
                    totalBeans = it.totalBeans,
                    defectiveBeans = 0,
                    defectScoreTotal = it.defectScore.toFloat(),
                    dominantDefect = "-",
                    scanDurationMs = 0L,
                    officerName = "-",
                    defects = emptyList()
                )
            }
        }
    }

    // ===== DEFECT LIST =====
    fun getDefects(historyId: Long): Flow<List<DefectRowUi>> {
        return defectDao.getDefectsByHistory(historyId).map { list ->
            list.mapIndexed { index, defect ->
                DefectRowUi(
                    no = index + 1,
                    defectName = defect.defectName,
                    defectScore = defect.defectValue,
                    count = defect.defectCount,
                    totalScore = defect.defectValue * defect.defectCount
                )
            }
        }
    }
}
