package com.example.runcoach.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.data.health.HealthConnectManager
import com.example.runcoach.MainActivity
import com.example.runcoach.presentation.receiver.RaceCountdownWidget
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
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now()
            val windowStart = today.minusDays(2)
            
            val startInstant = ZonedDateTime.of(windowStart.atStartOfDay(), zoneId).toInstant()
            val endInstant = ZonedDateTime.of(today.plusDays(1).atStartOfDay(), zoneId).toInstant()

            val allSessions = healthConnectManager.getRunningSessions(startInstant, endInstant)
            
            if (allSessions.isEmpty()) {
                return Result.success()
            }

            // Group sessions by date
            val sessionsByDate = allSessions.groupBy { session ->
                session.startTime.atZone(zoneId).toLocalDate()
            }
            
            val runningTypes = setOf("EASY", "LONG", "TEMPO", "RACE", "INTERVAL", "REPETITION", "RECOVERY")
            var syncedCount = 0
            var totalSyncedKm = 0.0
            val shifts = mutableListOf<String>()

            for ((sessionDate, sessions) in sessionsByDate) {
                val totalDistance = sessions.sumOf { it.distanceKm }
                val totalDuration = sessions.sumOf { it.durationMinutes }
                
                if (totalDistance < 0.5) continue // Coerce at least 0.5km to avoid random short tracks

                // Find matching uncompleted workout within +-1 day
                val candidateDates = listOf(
                    sessionDate.toString(),
                    sessionDate.minusDays(1).toString(),
                    sessionDate.plusDays(1).toString()
                )
                
                for (dateStr in candidateDates) {
                    val workout = dao.getWorkoutByDate(dateStr)
                    if (workout != null && !workout.isCompleted && !workout.isSkipped && workout.type in runningTypes) {
                        if (dateStr != sessionDate.toString()) {
                            // We need to shift this planned workout from dateStr to sessionDate!
                            val workoutOnSessionDate = dao.getWorkoutByDate(sessionDate.toString())
                            
                            val updatedMatchingWorkout = workout.copy(
                                date = sessionDate.toString(),
                                weekNumber = workoutOnSessionDate?.weekNumber ?: workout.weekNumber,
                                isCompleted = true,
                                actualDistanceKm = totalDistance,
                                actualDurationMin = totalDuration,
                                completedDate = sessions.first().startTime.toString(),
                                syncSource = "HEALTH_CONNECT"
                            )
                            
                            val updatedOtherWorkout = workoutOnSessionDate?.copy(
                                date = workout.date,
                                weekNumber = workout.weekNumber
                            )
                            
                            // Delete both old keys first
                            dao.delete(workout)
                            if (workoutOnSessionDate != null) {
                                dao.delete(workoutOnSessionDate)
                            }
                            
                            // Insert updated ones
                            dao.insert(updatedMatchingWorkout)
                            if (updatedOtherWorkout != null) {
                                dao.insert(updatedOtherWorkout)
                            }
                            
                            val formatter = DateTimeFormatter.ofPattern("dd/MM")
                            val sessionDateFormatted = sessionDate.format(formatter)
                            val originalDateFormatted = LocalDate.parse(workout.date).format(formatter)
                            shifts.add("Chuyển bài tập ngày $originalDateFormatted sang ngày $sessionDateFormatted (${workout.description})")
                            
                            com.example.runcoach.utils.AppLogger.i(
                                "SyncWorker: Shifted and completed workout from $dateStr to $sessionDate"
                            )
                        } else {
                            // Match on same day, normal update
                            val updated = workout.copy(
                                isCompleted = true,
                                actualDistanceKm = totalDistance,
                                actualDurationMin = totalDuration,
                                completedDate = sessions.first().startTime.toString(),
                                syncSource = "HEALTH_CONNECT"
                            )
                            dao.update(updated)
                            com.example.runcoach.utils.AppLogger.i(
                                "SyncWorker: Synced ${totalDistance}km on exact date $sessionDate"
                            )
                        }
                        syncedCount++
                        totalSyncedKm += totalDistance
                        break // Found a match for this session, move to next date
                    }
                }
            }

            if (syncedCount > 0) {
                // Send widget update broadcast
                val intent = Intent(applicationContext, RaceCountdownWidget::class.java).apply {
                    action = "com.example.runcoach.ACTION_REFRESH_WIDGET"
                }
                applicationContext.sendBroadcast(intent)

                // Show system notification
                var notifMsg = "Đã đồng bộ thành công $syncedCount buổi tập! (Tổng: ${"%,.1f".format(totalSyncedKm)} km)"
                if (shifts.isNotEmpty()) {
                    notifMsg += "\nĐã tự động điều chỉnh giáo án tuần này theo thực tế tập luyện."
                }
                showSyncNotification(applicationContext, "Hoàn thành buổi chạy mới!", notifMsg)
            }

            return Result.success()
        } catch (e: Exception) {
            com.example.runcoach.utils.AppLogger.e("SyncWorker doWork failed", e)
            return Result.retry()
        }
    }

    private fun showSyncNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "health_connect_sync"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Đồng bộ Health Connect",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo khi hoàn thành/đồng bộ bài tập từ Health Connect"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(2002, builder.build())
    }
}
