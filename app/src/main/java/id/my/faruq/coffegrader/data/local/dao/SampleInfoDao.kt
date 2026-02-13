package id.my.faruq.coffegrader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleInfoDao {

    @Insert
    suspend fun insert(sample: SampleInfoEntity): Long

    @Query("SELECT * FROM sample_info ORDER BY timestamp DESC")
    fun observeAllSamples(): Flow<List<SampleInfoEntity>>
}
