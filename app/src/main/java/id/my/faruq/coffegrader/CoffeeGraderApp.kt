package id.my.faruq.coffegrader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.my.faruq.coffegrader.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class CoffeeGraderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runSeedIfNeeded()
    }

    private fun runSeedIfNeeded() {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SeedEntryPoint::class.java
        )
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                entryPoint.historyRepository().seedDummyIfEmpty()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SeedEntryPoint {
    fun historyRepository(): HistoryRepository
}
