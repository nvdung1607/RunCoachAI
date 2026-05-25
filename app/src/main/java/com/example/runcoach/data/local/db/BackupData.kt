package com.example.runcoach.data.local.db

import com.example.runcoach.data.local.preferences.UserPreferences

data class BackupData(
    val version: Int = 1,
    val preferences: UserPreferences,
    val workouts: List<WorkoutEntity>
)
