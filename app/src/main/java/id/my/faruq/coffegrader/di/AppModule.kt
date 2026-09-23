package id.my.faruq.coffegrader.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.my.faruq.coffegrader.data.local.AppDatabase
import id.my.faruq.coffegrader.data.local.dao.ScanHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Database
//    @Provides
//    @Singleton
//    fun provideDatabase(
//        @ApplicationContext context: Context
//    ): AppDatabase {
//        return Room.databaseBuilder(
//            context,
//            AppDatabase::class.java,
//            "coffeegrader_db"
//        )
//            .fallbackToDestructiveMigration()
//            .build()
//    }

    // DAO

}
