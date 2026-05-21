package com.example.runcoach.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.data.health.HealthConnectManager
import com.example.runcoach.data.local.db.WorkoutEntity
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
            // Get all workouts from local DB
            // We can block/suspend to query all workouts
            val workouts = dao.getWorkoutByDate("dummy") // just dummy check or we can write a DAO method to get all list
            // Since we need to get all workouts, let's write a standard non-flow method in Dao if needed,
            // or query using a flow and take the first item.
            val allWorkouts = dao.getAllWorkoutsFlow().first()
            val uncompletedRunningWorkouts = allWorkouts.filter { 
                !it.isCompleted && (it.type == "EASY" || it.type == "LONG" || it.type == "TEMPO" || it.type == "RACE")
            }

            if (uncompletedRunningWorkouts.isEmpty()) {
                return Result.success()
            }

            val zoneId = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            for (workout in uncompletedRunningWorkouts) {
                val localDate = LocalDate.parse(workout.date, formatter)
                
                // Define start and end of the day in UTC Instants
                val dayStartInstant = ZonedDateTime.of(localDate.atStartOfDay(), zoneId).toInstant()
                val dayEndInstant = ZonedDateTime.of(localDate.plusDays(1).atStartOfDay(), zoneId).toInstant()

                val sessions = healthConnectManager.getRunningSessions(dayStartInstant, dayEndInstant)
                
                if (sessions.isNotEmpty()) {
                    // Match the run! We aggregate distances and durations if there are multiple runs on the same day.
                    val totalDistance = sessions.sumOf { it.distanceKm }
                    val totalDuration = sessions.sumOf { it.durationMinutes }
                    
                    // If they ran at least 0.5km, we count it as a valid completion of the workout
                    if (totalDistance >= 0.5) {
                        val updatedWorkout = workout.copy(
                            isCompleted = true,
                            actualDistanceKm = totalDistance,
                            actualDurationMin = totalDuration,
                            completedDate = sessions.first().startTime.toString(),
                            syncSource = "HEALTH_CONNECT"
                        )
                        dao.update(updatedWorkout)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
