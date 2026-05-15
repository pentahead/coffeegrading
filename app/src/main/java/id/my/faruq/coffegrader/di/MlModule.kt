package id.my.faruq.coffegrader.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.my.faruq.coffegrader.ml.CoffeeInferenceEngine
import javax.inject.Singleton

/**
 * Hilt module untuk ML layer.
 * CoffeeInferenceEngine sudah anotasi @Singleton + @Inject constructor,
 * sehingga Hilt otomatis membuatnya — module ini hanya memastikan
 * ApplicationContext tersedia (sudah disediakan Hilt secara default,
 * tapi eksplisit di sini untuk kejelasan).
 *
 * Jika CoffeeInferenceEngine hanya butuh @ApplicationContext Context,
 * module ini bisa dikosongkan. Disediakan sebagai hook jika ke depan
 * perlu custom Interpreter.Options atau model multi-backend.
 */
@Module
@InstallIn(SingletonComponent::class)
object MlModule
// Tidak ada @Provides tambahan — Hilt meng-inject @ApplicationContext Context
// langsung ke constructor CoffeeInferenceEngine karena sudah @Singleton @Inject.
