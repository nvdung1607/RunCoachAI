package com.example.runcoach.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val date: String,     // YYYY-MM-DD
    val weekNumber: Int,              // 1 to 13+
    val type: String,                 // "EASY", "LONG", "TEMPO", "RECOVERY", "REST", "RACE", "CT"
    val targetDistanceKm: Double,
    val targetPaceSec: Int,           // Target pace in seconds/km (e.g. 480 = 8:00/km)
    val description: String,
    val instructions: String,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,   // True if user rescheduled this to another day
    val rescheduledFromDate: String? = null, // Original date if this is a rescheduled workout
    val actualDistanceKm: Double = 0.0,
    val actualDurationMin: Double = 0.0,
    val completedDate: String? = null,
    val syncSource: String? = null    // "HEALTH_CONNECT", "MANUAL", null
)
