package com.example.runcoach.presentation.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
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
        if (intent.action == "com.example.runcoach.ACTION_REFRESH_WIDGET") {
            com.example.runcoach.utils.AppLogger.d("RaceCountdownWidget: Auto/Manual refresh intent received")
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
            views.setTextViewText(R.id.widget_days_count, "📅")
            views.setTextViewText(R.id.widget_days_label, "Chưa thiết lập")
            views.setTextViewText(R.id.widget_target, "Chạm để cài đặt")
            
            views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_none)
            views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_text_secondary))
            views.setTextViewText(R.id.widget_workout_title, "Chưa có giáo án")
            views.setTextViewText(R.id.widget_workout_desc, "Mở ứng dụng để tạo lịch tập")
        } else {
            try {
                val today = LocalDate.now()
                val raceDate = LocalDate.parse(raceDateStr)
                val daysRemaining = ChronoUnit.DAYS.between(today, raceDate)

                if (daysRemaining < 0) {
                    views.setTextViewText(R.id.widget_days_count, "🏁")
                    views.setTextViewText(R.id.widget_days_label, "Đã đến ngày đua")
                    views.setTextViewText(R.id.widget_target, "Cự ly: $targetDistance km")
                } else {
                    views.setTextViewText(R.id.widget_days_count, daysRemaining.toString())
                    views.setTextViewText(R.id.widget_days_label, "ngày còn lại")
                    views.setTextViewText(R.id.widget_target, "Mục tiêu: $targetDistance km")
                }

                // Update Today's Workout Card
                if (todayWorkout != null) {
                    if (todayWorkout.type in listOf("REST", "CT")) {
                        views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_rest)
                        views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_accent_indigo))
                        views.setTextViewText(R.id.widget_workout_title, "NGHỈ NGƠI")
                        views.setTextViewText(R.id.widget_workout_desc, "Để cơ bắp phục hồi")
                    } else {
                        if (todayWorkout.isCompleted) {
                            views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_check)
                            views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_accent_green))
                            views.setTextViewText(R.id.widget_workout_title, "HOÀN THÀNH")
                            views.setTextViewText(
                                R.id.widget_workout_desc, 
                                "Đã chạy: ${String.format(java.util.Locale.US, "%.1f", todayWorkout.actualDistanceKm)} km"
                            )
                        } else if (todayWorkout.isSkipped) {
                            views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_skipped)
                            views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_accent_indigo))
                            views.setTextViewText(R.id.widget_workout_title, "ĐÃ DỜI LỊCH")
                            views.setTextViewText(R.id.widget_workout_desc, "Chạy bù vào hôm sau")
                        } else {
                            views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_run)
                            views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_accent_blue))
                            views.setTextViewText(R.id.widget_workout_title, "${todayWorkout.type} RUN")
                            
                            val distStr = String.format(java.util.Locale.US, "%.1f", todayWorkout.targetDistanceKm)
                            val paceStr = formatPace(todayWorkout.targetPaceSec)
                            views.setTextViewText(R.id.widget_workout_desc, "$distStr km • Pace $paceStr")
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_none)
                    views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_text_secondary))
                    views.setTextViewText(R.id.widget_workout_title, "KHÔNG CÓ LỊCH")
                    views.setTextViewText(R.id.widget_workout_desc, "Nghỉ ngơi hoặc chạy tự do")
                }
                
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_days_count, "⚠️")
                views.setTextViewText(R.id.widget_days_label, "Lỗi định dạng")
                
                views.setImageViewResource(R.id.widget_workout_icon, R.drawable.ic_widget_none)
                views.setInt(R.id.widget_workout_icon, "setColorFilter", context.getColor(R.color.widget_accent_amber))
                views.setTextViewText(R.id.widget_workout_title, "Có lỗi xảy ra")
                views.setTextViewText(R.id.widget_workout_desc, "Vui lòng mở ứng dụng")
            }
        }

        // PendingIntent to launch MainActivity when clicking any part of the widget
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Bind click layout targets
        views.setOnClickPendingIntent(R.id.widget_countdown_container, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_workout_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun formatPace(paceSeconds: Int): String {
        if (paceSeconds <= 0) return "--"
        val mins = paceSeconds / 60
        val secs = paceSeconds % 60
        return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
    }
}
