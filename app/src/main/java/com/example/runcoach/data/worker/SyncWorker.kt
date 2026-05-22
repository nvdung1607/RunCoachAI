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
            var hasMismatch = false

            for ((sessionDate, sessions) in sessionsByDate) {
                val totalDistance = sessions.sumOf { it.distanceKm }
                val totalDuration = sessions.sumOf { it.durationMinutes }
                
                if (totalDistance < 0.5) continue // Coerce at least 0.5km to avoid random short tracks

                // Try exact match first
                val exactWorkout = dao.getWorkoutByDate(sessionDate.toString())
                if (exactWorkout != null && !exactWorkout.isCompleted && !exactWorkout.isSkipped && exactWorkout.type in runningTypes) {
                    // Match on exact date, normal update
                    val updated = exactWorkout.copy(
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
                    syncedCount++
                    totalSyncedKm += totalDistance
                } else {
                    // No exact match, check for shift candidates within +-1 day
                    val shiftDates = listOf(
                        sessionDate.minusDays(1).toString(),
                        sessionDate.plusDays(1).toString()
                    )
                    var foundShiftCandidate = false
                    for (dateStr in shiftDates) {
                        val workout = dao.getWorkoutByDate(dateStr)
                        if (workout != null && !workout.isCompleted && !workout.isSkipped && workout.type in runningTypes) {
                            foundShiftCandidate = true
                            break
                        }
                    }
                    if (foundShiftCandidate) {
                        hasMismatch = true
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
                val notifMsg = "Đã đồng bộ thành công $syncedCount buổi tập! (Tổng: ${"%,.1f".format(totalSyncedKm)} km)"
                showSyncNotification(applicationContext, "Hoàn thành buổi chạy mới!", notifMsg, 2002)
            }

            if (hasMismatch) {
                // Show notification prompting user to sync manually
                showSyncNotification(
                    applicationContext,
                    "Lịch tập luyện có sự thay đổi?",
                    "Phát hiện buổi chạy mới không khớp lịch. Hãy mở ứng dụng để đồng bộ và cập nhật giáo án.",
                    2003
                )
            }

            return Result.success()
        } catch (e: Exception) {
            com.example.runcoach.utils.AppLogger.e("SyncWorker doWork failed", e)
            return Result.retry()
        }
    }

    private fun showSyncNotification(context: Context, title: String, message: String, notificationId: Int = 2002) {
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

        notificationManager.notify(notificationId, builder.build())
    }
}
