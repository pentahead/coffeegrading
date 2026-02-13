package id.my.faruq.coffegrader.data.repository

import id.my.faruq.coffegrader.data.local.dao.ScanDefectDao
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import id.my.faruq.coffegrader.data.local.entity.ScanDefectEntity
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ScanRepository @Inject constructor(
    private val historyDao: ScanHistoryDao,
    private val defectDao: ScanDefectDao
) {

    // ✅ Simpan hasil scan baru
    suspend fun saveScanResult(
        batchName: String,
        dateTime: String,
        totalBeans: Int,
        defectScore: Int,
        gradeText: String,
        defects: List<ScanDefectEntity>
    ): Long {

        // 1. Insert history dulu
        val historyId = historyDao.insert(
            ScanHistoryEntity(
                batchName = batchName,
                dateTime = dateTime,
                totalBeans = totalBeans,
                defectScore = defectScore,
                gradeText = gradeText
            )
        )

        // 2. Insert semua defect terkait scan ini
        defectDao.insertAll(
            defects.map {
                it.copy(historyId = historyId)
            }
        )

        return historyId
    }

    // ✅ Ambil semua history list
    fun getAllHistory(): Flow<List<ScanHistoryEntity>> {
        return historyDao.getAllHistory()
    }

    // ✅ Ambil detail scan berdasarkan id
    suspend fun getHistoryDetail(historyId: Long): ScanHistoryEntity? {
        return historyDao.getById(historyId)
    }

    // ✅ Ambil defects berdasarkan historyId
    fun getDefectsByHistory(historyId: Long): Flow<List<ScanDefectEntity>> {
        return defectDao.getDefectsByHistory(historyId)
    }
}
