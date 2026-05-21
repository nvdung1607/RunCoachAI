package com.example.runcoach.presentation.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.runcoach.MainActivity
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.domain.plan.VdotCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class WorkoutReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.example.runcoach.ACTION_WORKOUT_REMINDER"
        const val CHANNEL_ID = "workout_reminders"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"

        fun scheduleDailyAlarm(context: Context, hour: Int = 6, minute: Int = 0) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If this time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback for devices that restrict exact alarms
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RunCoachApplication

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule alarm after device reboot using saved preferences
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = app?.userPreferencesRepository?.userPreferencesFlow?.first()
                scheduleDailyAlarm(
                    context,
                    prefs?.notificationHour ?: 6,
                    prefs?.notificationMinute ?: 0
                )
            }
            return
        }

        if (intent.action == ACTION_REMINDER) {
            val hour = intent.getIntExtra(EXTRA_HOUR, 6)
            val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

            CoroutineScope(Dispatchers.IO).launch {
                val preferences = app?.userPreferencesRepository?.userPreferencesFlow?.first()
                if (preferences?.isNotificationEnabled == false) {
                    // Still reschedule so it's ready when re-enabled
                    scheduleDailyAlarm(context, hour, minute)
                    return@launch
                }

                val dao = app?.database?.workoutDao()
                val todayDate = LocalDate.now().toString()
                val workout = dao?.getWorkoutByDate(todayDate)

                if (workout != null && !workout.isCompleted && !workout.isSkipped) {
                    val (title, message) = when (workout.type) {
                        "EASY" -> {
                            val paceStr = VdotCalculator.formatPace(workout.targetPaceSec)
                            "🏃 Lịch chạy nhẹ hôm nay!" to
                                "Easy Run ${workout.targetDistanceKm}km — Pace mục tiêu: $paceStr. Hãy giữ nhịp thở đều!"
                        }
                        "LONG" -> {
                            val paceStr = VdotCalculator.formatPace(workout.targetPaceSec)
                            "🏃‍♂️ Chạy dài cuối tuần!" to
                                "Long Run ${workout.targetDistanceKm}km — Pace: $paceStr. Nhớ mang nước theo nhé!"
                        }
                        "TEMPO" -> {
                            val paceStr = VdotCalculator.formatPace(workout.targetPaceSec)
                            "🔥 Hôm nay có Tempo Run!" to
                                "Tempo ${workout.targetDistanceKm}km — Pace mục tiêu: $paceStr. Cố gắng lên!"
                        }
                        "RECOVERY" -> {
                            "🌿 Chạy phục hồi hôm nay" to
                                "Recovery Run ${workout.targetDistanceKm}km — Chạy thật nhẹ nhàng để phục hồi cơ thể."
                        }
                        "RACE" -> {
                            "🏁 RACE DAY! Hôm nay là ngày đua!" to
                                "21km Half Marathon — Chúc bạn chinh phục thành công! Hãy khởi động kỹ 💪"
                        }
                        "CT" -> {
                            "🏊 Tập bổ trợ hôm nay" to
                                "Cross Training — Bơi lội, yoga, hoặc đạp xe 30-45 phút để duy trì thể lực."
                        }
                        "REST" -> {
                            "😴 Hôm nay bạn được nghỉ!" to
                                "Ngày nghỉ ngơi — Ăn uống đầy đủ và ngủ đủ giấc để phục hồi nhé!"
                        }
                        else -> null to null
                    }
                    if (title != null && message != null) {
                        showNotification(context, title, message)
                    }
                }

                // Reschedule for the next day
                scheduleDailyAlarm(context, hour, minute)
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lịch nhắc chạy bộ",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở các bài tập chạy trong ngày"
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
