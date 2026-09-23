package id.my.faruq.coffegrader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.my.faruq.coffegrader.data.local.entity.GradingEntity

@Dao
interface GradingDao {

    @Insert
    suspend fun insert(data: GradingEntity)

    @Query("SELECT * FROM grading_results ORDER BY timestamp DESC")
    suspend fun getAll(): List<GradingEntity>
}
