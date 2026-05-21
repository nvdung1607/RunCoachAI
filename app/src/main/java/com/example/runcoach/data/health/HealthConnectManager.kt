package com.example.runcoach.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

data class RunningSession(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val distanceKm: Double,
    val durationMinutes: Double
)

class HealthConnectManager(private val context: Context) {

    val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: run {
            android.util.Log.w("RunCoachHC", "[HealthConnectManager] healthConnectClient is null (SDK not available)!")
            return false
        }
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val isAllGranted = granted.containsAll(requiredPermissions)
            com.example.runcoach.utils.AppLogger.d("[HealthConnectManager] Checked permissions:")
            com.example.runcoach.utils.AppLogger.d("  - Required: $requiredPermissions")
            com.example.runcoach.utils.AppLogger.d("  - Granted: $granted")
            com.example.runcoach.utils.AppLogger.d("  - containsAll: $isAllGranted")
            if (!isAllGranted) {
                val missing = requiredPermissions.filter { it !in granted }
                android.util.Log.w("RunCoachHC", "  - Missing: $missing")
            }
            isAllGranted
        } catch (e: Exception) {
            android.util.Log.e("RunCoachHC", "[HealthConnectManager] Error checking permissions in hasPermissions()", e)
            false
        }
    }

    /**
     * Queries running activities between startTime and endTime.
     */
    suspend fun getRunningSessions(startTime: Instant, endTime: Instant): List<RunningSession> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasPermissions()) return emptyList()

        return try {
            // 1. Query all exercise sessions
            val exerciseRequest = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val exerciseResponse = client.readRecords(exerciseRequest)
            val runningRecords = exerciseResponse.records.filter {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            }

            val sessions = mutableListOf<RunningSession>()

            for (record in runningRecords) {
                // 2. Query distance records within the running session time range to calculate total distance
                val distanceRequest = ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                )
                val distanceResponse = client.readRecords(distanceRequest)
                
                // Sum distances (Health Connect stores distance in meters)
                val totalMeters = distanceResponse.records.sumOf { it.distance.inMeters }
                val distanceKm = totalMeters / 1000.0
                
                val duration = Duration.between(record.startTime, record.endTime)
                val durationMinutes = duration.toMillis() / 60000.0

                sessions.add(
                    RunningSession(
                        id = record.metadata.id,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        distanceKm = if (distanceKm > 0.0) distanceKm else 0.0, // fallback if no distance records found
                        durationMinutes = durationMinutes
                    )
                )
            }
            sessions
        } catch (e: Exception) {
            emptyList()
        }
    }
}
