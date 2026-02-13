package id.my.faruq.coffegrader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.my.faruq.coffegrader.data.local.entity.ScanDefectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDefectDao {

    @Insert
    suspend fun insertAll(defects: List<ScanDefectEntity>)

    @Query("SELECT * FROM scan_defect WHERE historyId = :historyId")
    fun getDefectsByHistory(historyId: Long): Flow<List<ScanDefectEntity>>


    @Query("SELECT * FROM scan_defect WHERE historyId = :historyId")
    suspend fun getDefects(historyId: Long): List<ScanDefectEntity>

}
