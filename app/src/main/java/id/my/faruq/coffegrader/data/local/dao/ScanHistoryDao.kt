package id.my.faruq.coffegrader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    // ✅ Insert scan history
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ScanHistoryEntity): Long

    // ✅ Ambil semua riwayat
    @Query("SELECT * FROM scan_history ORDER BY id DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    // ✅ Hitung jumlah data (untuk dummy seed)
    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun countHistory(): Int

    // ✅ Ambil detail berdasarkan id scan
    @Query("SELECT * FROM scan_history WHERE id = :historyId LIMIT 1")
    suspend fun getById(historyId: Long): ScanHistoryEntity?

    @Query("""
    SELECT * FROM scan_history
    WHERE id = :scanId
    LIMIT 1
""")
    fun getHistoryDetail(scanId: Long): Flow<ScanHistoryEntity?>

    @Query("UPDATE scan_history SET batchName = :batchName WHERE id = :historyId")
    suspend fun updateBatchName(historyId: Long, batchName: String)
}
