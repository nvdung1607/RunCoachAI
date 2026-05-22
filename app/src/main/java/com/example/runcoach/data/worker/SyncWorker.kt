package com.example.runcoach.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.data.health.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RunCoachApplication ?: return Result.failure()
        val dao = app.database.workoutDao()
        val healthConnectManager = HealthConnectManager(applicationContext)

        if (!healthConnectManager.hasPermissions()) {
            return Result.success() // No permissions yet, skip sync
        }

        try {
            val allWorkouts = dao.getAllWorkoutsFlow().first()
            
            // Include all running/workout types that should be synced
            val runningTypes = setOf("EASY", "LONG", "TEMPO", "RACE", "INTERVAL", "REPETITION", "RECOVERY")
            val uncompletedRunningWorkouts = allWorkouts.filter { 
                !it.isCompleted && !it.isSkipped && it.type in runningTypes
            }

            if (uncompletedRunningWorkouts.isEmpty()) {
                return Result.success()
            }

            val zoneId = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val today = LocalDate.now()

            // Option B: Query HC for last 3 days and match to uncompleted workouts within +-1 day
            // Build a window of the last 3 days of HC data
            val windowStart = today.minusDays(2)
            val windowStartInstant = ZonedDateTime.of(windowStart.atStartOfDay(), zoneId).toInstant()
            val windowEndInstant = ZonedDateTime.of(today.plusDays(1).atStartOfDay(), zoneId).toInstant()
            
            val allRecentSessions = healthConnectManager.getRunningSessions(windowStartInstant, windowEndInstant)
            
            if (allRecentSessions.isEmpty()) {
                return Result.success()
            }

            // Group sessions by the date they occurred (local date)
            val sessionsByDate = allRecentSessions.groupBy { session ->
                session.startTime.atZone(zoneId).toLocalDate()
            }

            // For each date that has HC sessions, try to find a matching uncompleted workout
            // within +-1 day of that session date
            for ((sessionDate, sessions) in sessionsByDate) {
                val totalDistance = sessions.sumOf { it.distanceKm }
                val totalDuration = sessions.sumOf { it.durationMinutes }
                
                if (totalDistance < 0.5) continue // Skip too-short runs

                // Find best matching uncompleted workout: exact date first, then +-1 day
                val candidateDates = listOf(
                    sessionDate.toString(),
                    sessionDate.minusDays(1).toString(),
                    sessionDate.plusDays(1).toString()
                )
                
                val matchingWorkout = candidateDates
                    .mapNotNull { dateStr -> uncompletedRunningWorkouts.find { it.date == dateStr } }
                    .firstOrNull()
                
                if (matchingWorkout != null && !matchingWorkout.isCompleted) {
                    val updatedWorkout = matchingWorkout.copy(
                        isCompleted = true,
                        actualDistanceKm = totalDistance,
                        actualDurationMin = totalDuration,
                        completedDate = sessions.first().startTime.toString(),
                        syncSource = "HEALTH_CONNECT"
                    )
                    dao.update(updatedWorkout)
                    com.example.runcoach.utils.AppLogger.i(
                        "SyncWorker: Matched ${totalDistance}km run on $sessionDate to workout on ${matchingWorkout.date}"
                    )
                }
            }
            
            // Send widget update broadcast
            val intent = android.content.Intent(applicationContext, com.example.runcoach.presentation.receiver.RaceCountdownWidget::class.java).apply {
                action = "com.example.runcoach.ACTION_REFRESH_WIDGET"
            }
            applicationContext.sendBroadcast(intent)

            return Result.success()
        } catch (e: Exception) {
            com.example.runcoach.utils.AppLogger.e("SyncWorker doWork failed", e)
            return Result.retry()
        }
    }
}
