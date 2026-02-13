package id.my.faruq.coffegrader.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.my.faruq.coffegrader.data.local.AppDatabase
import id.my.faruq.coffegrader.data.local.dao.ScanDefectDao
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import id.my.faruq.coffegrader.data.repository.ScanRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ✅ Database
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "coffeegrader_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    // ✅ Dao
    @Provides
    fun provideHistoryDao(db: AppDatabase): ScanHistoryDao =
        db.scanHistoryDao()

    @Provides
    fun provideDefectDao(db: AppDatabase): ScanDefectDao =
        db.scanDefectDao()

    // ✅ Repository
    @Provides
    @Singleton
    fun provideScanRepository(
        historyDao: ScanHistoryDao,
        defectDao: ScanDefectDao
    ): ScanRepository {
        return ScanRepository(historyDao, defectDao)
    }
}
