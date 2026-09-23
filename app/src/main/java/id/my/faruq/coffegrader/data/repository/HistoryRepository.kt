package id.my.faruq.coffegrader.data.repository

import androidx.compose.ui.graphics.Color
import id.my.faruq.coffegrader.data.local.dao.ScanDefectDao
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity
import id.my.faruq.coffegrader.util.GradeColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Info sampel (SNI) untuk ditampilkan di detail riwayat */
data class SampleInfoUi(
    val batchId: String,
    val coffeeType: String,
    val processingMethod: String,
    val beanSize: String,
    val beanShape: String,
    val sortationType: String,
    val hasInsect: Boolean,
    val hasMoldSmell: Boolean,
    val moistureContent: Float,
    val dirtContent: Float,
    val origin: String?
)

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
    val defectScoreTotal: Double,
    val dominantDefect: String,
    val scanDurationMs: Long,
    val officerName: String,
    val defects: List<DefectRowUi>,
    val sampleInfo: SampleInfoUi? = null,
    val imagePath: String? = null
)

/** Satu baris cacat di tabel detail */
data class DefectRowUi(
    val no: Int,
    val defectName: String,
    val defectScore: Double,
    val count: Int,
    val totalScore: Double
)

class HistoryRepository @Inject constructor(
    private val historyDao: ScanHistoryDao,
    private val defectDao: ScanDefectDao,
    private val sampleRepository: SampleRepository
) {

    fun getAllHistory() = historyDao.getAllHistory()

    suspend fun updateBatchName(historyId: Long, batchName: String) {
        historyDao.updateBatchName(historyId, batchName)
    }

    suspend fun seedDummyIfEmpty() {
        val count = historyDao.countHistory()

        if (count == 0) {
            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_001",
                    dateTime = "08:15 / 2025-11-30",
                    totalBeans = 132,
                    defectScore = 11.0,
                    gradeText = "1",
                    imagePath = null,
                    thumbnailPath = null
                )
            )

            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_002",
                    dateTime = "08:16 / 2025-11-30",
                    totalBeans = 132,
                    defectScore = 33.0,
                    gradeText = "4a",
                    imagePath = null,
                    thumbnailPath = null
                )
            )

            historyDao.insert(
                ScanHistoryEntity(
                    batchName = "SCAN_003",
                    dateTime = "08:18 / 2025-11-30",
                    totalBeans = 128,
                    defectScore = 92.0,
                    gradeText = "5",
                    imagePath = null,
                    thumbnailPath = null
                )
            )
        }
    }

    // ===== DETAIL UTAMA (dengan info sampel SNI jika ada) =====
    fun getDetail(historyId: Long): Flow<HistoryDetailUi?> = flow {
        historyDao.getHistoryDetail(historyId).collect { entity ->
            if (entity != null) {
                val sample = entity.sampleInfoId?.let { id ->
                    sampleRepository.getById(id)
                }
                // dateTime: "dd-MM-yyyy HH:mm:ss" -> waktu = jam:menit:detik, tanggal = dd-MM-yyyy
                val dateTime = entity.dateTime
                val (dateText, timeText) = if (dateTime.contains(" ")) {
                    val parts = dateTime.split(" ", limit = 2)
                    Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { dateTime })
                } else {
                    val parts = dateTime.split("/").map { it.trim() }
                    Pair(parts.getOrElse(1) { "" }, parts.getOrElse(0) { dateTime })
                }
                emit(
                    HistoryDetailUi(
                        id = entity.id.toString(),
                        batchName = entity.batchName,
                        timeText = timeText,
                        dateText = dateText,
                        gradeText = entity.gradeText,
                        themeColor = Color(GradeColors.colorFor(entity.gradeText)),
                        totalBeans = entity.totalBeans,
                        defectiveBeans = 0,
                        defectScoreTotal = entity.defectScore,
                        dominantDefect = "-",
                        scanDurationMs = entity.scanDurationMs,
                        officerName = "-",
                        defects = emptyList(),
                        sampleInfo = sample?.toSampleInfoUi(),
                        imagePath = entity.imagePath
                    )
                )
            } else {
                emit(null)
            }
        }
    }

    private fun SampleInfoEntity.toSampleInfoUi() = SampleInfoUi(
        batchId = batchId,
        coffeeType = coffeeType,
        processingMethod = processingMethod,
        beanSize = beanSize,
        beanShape = beanShape,
        sortationType = sortationType,
        hasInsect = hasInsect,
        hasMoldSmell = hasMoldSmell,
        moistureContent = moistureContent,
        dirtContent = dirtContent,
        origin = origin
    )

    // ===== DEFECT LIST =====
    fun getDefects(historyId: Long): Flow<List<DefectRowUi>> {
        return defectDao.getDefectsByHistory(historyId).map { list ->
            list.mapIndexed { index, defect ->
                DefectRowUi(
                    no = index + 1,
                    defectName = defect.defectName,
                    defectScore = defect.defectValue,
                    count = defect.defectCount,
                    totalScore = defect.defectValue * defect.defectCount.toDouble()
                )
            }
        }
    }
}
