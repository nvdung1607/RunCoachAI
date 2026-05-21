package com.example.runcoach

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.runcoach.data.local.db.RunDatabase
import com.example.runcoach.data.local.preferences.UserPreferencesRepository
import com.example.runcoach.data.worker.SyncWorker
import java.util.concurrent.TimeUnit

class RunCoachApplication : Application() {

    val database: RunDatabase by lazy {
        RunDatabase.getDatabase(this)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleSyncWork()
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "runcoach_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
