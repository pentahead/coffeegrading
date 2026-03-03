package id.my.faruq.coffegrader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.my.faruq.coffegrader.data.local.dao.SampleInfoDao
import id.my.faruq.coffegrader.data.local.dao.ScanDefectDao
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity
import id.my.faruq.coffegrader.data.local.entity.ScanDefectEntity
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity

@Database(
    entities = [
        SampleInfoEntity::class,
        ScanHistoryEntity::class,
        ScanDefectEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sampleInfoDao(): SampleInfoDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun scanDefectDao(): ScanDefectDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffeegrader_db"
                )
                    // sementara biar gak error migration
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
