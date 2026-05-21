package com.example.runcoach.presentation.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.runcoach.MainActivity
import com.example.runcoach.R
import com.example.runcoach.data.local.preferences.UserPreferencesRepository
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.data.local.db.WorkoutEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RaceCountdownWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repository = UserPreferencesRepository(context.applicationContext)
        val app = context.applicationContext as? RunCoachApplication
        val workoutDao = app?.database?.workoutDao()
        
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = repository.userPreferencesFlow.first()
                val raceDateStr = prefs.raceDate
                val targetDist = prefs.targetDistance
                
                val todayDateStr = LocalDate.now().toString()
                val todayWorkout = workoutDao?.getWorkoutByDate(todayDateStr)

                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, raceDateStr, targetDist, todayWorkout)
                }
            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("RaceCountdownWidget: Exception during update", e)
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle manual refresh
        if (intent.action == "com.example.runcoach.ACTION_REFRESH_WIDGET") {
            com.example.runcoach.utils.AppLogger.d("RaceCountdownWidget: Manual refresh intent received")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RaceCountdownWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            onUpdate(context, appWidgetManager, ids)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        raceDateStr: String,
        targetDistance: Int,
        todayWorkout: WorkoutEntity?
    ) {
        val views = RemoteViews(context.packageName, R.layout.race_countdown_widget)

        if (raceDateStr.isEmpty()) {
            views.setTextViewText(R.id.widget_days_count, "--")
            views.setTextViewText(R.id.widget_days_label, "Chưa cài đặt giáo án")
            views.setTextViewText(R.id.widget_target, "Nhấp để thiết lập")
            views.setTextViewText(R.id.widget_workout_title, "Chưa có giáo án")
            views.setTextViewText(R.id.widget_workout_desc, "Vào app để thiết lập mục tiêu và tạo giáo án.")
        } else {
            try {
                val today = LocalDate.now()
                val raceDate = LocalDate.parse(raceDateStr)
                val daysRemaining = ChronoUnit.DAYS.between(today, raceDate)

                if (daysRemaining < 0) {
                    views.setTextViewText(R.id.widget_days_count, "🏁")
                    views.setTextViewText(R.id.widget_days_label, "Đã đến ngày đua!")
                    views.setTextViewText(R.id.widget_target, "Cự ly: ${targetDistance}km")
                } else {
                    views.setTextViewText(R.id.widget_days_count, daysRemaining.toString())
                    views.setTextViewText(R.id.widget_days_label, "ngày còn lại")
                    views.setTextViewText(R.id.widget_target, "Mục tiêu: ${targetDistance}km")
                }
                
                // Update Today's Workout
                if (todayWorkout != null) {
                    if (todayWorkout.type in listOf("REST", "CT")) {
                        views.setTextViewText(R.id.widget_workout_title, "Ngày nghỉ (Rest)")
                        views.setTextViewText(R.id.widget_workout_desc, "Hôm nay không có lịch chạy, hãy để cơ bắp phục hồi.")
                    } else {
                        views.setTextViewText(R.id.widget_workout_title, "🏃 ${todayWorkout.type} - ${todayWorkout.description}")
                        if (todayWorkout.isCompleted) {
                            views.setTextViewText(R.id.widget_workout_desc, "✅ Đã hoàn thành: ${todayWorkout.actualDistanceKm}km")
                        } else if (todayWorkout.isSkipped) {
                            views.setTextViewText(R.id.widget_workout_desc, "⏩ Đã dời lịch tập")
                        } else {
                            views.setTextViewText(R.id.widget_workout_desc, "Mục tiêu: ${todayWorkout.targetDistanceKm}km\n${todayWorkout.instructions}")
                        }
                    }
                } else {
                    views.setTextViewText(R.id.widget_workout_title, "Chưa có bài tập")
                    views.setTextViewText(R.id.widget_workout_desc, "Vui lòng làm mới hoặc vào app để kiểm tra.")
                }
                
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_days_count, "Error")
                views.setTextViewText(R.id.widget_days_label, "Sai định dạng ngày")
            }
        }

        // PendingIntent to launch MainActivity when clicking the widget
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_days_count, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_days_label, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_target, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_workout_container, pendingIntent)

        // PendingIntent for refresh button
        val refreshIntent = Intent(context, RaceCountdownWidget::class.java).apply {
            action = "com.example.runcoach.ACTION_REFRESH_WIDGET"
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
