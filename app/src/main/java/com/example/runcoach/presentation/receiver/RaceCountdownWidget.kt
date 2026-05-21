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

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = repository.userPreferencesFlow.first()
            val raceDateStr = prefs.raceDate
            val targetDist = prefs.targetDistance

            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, raceDateStr, targetDist)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle direct updates if triggered manually or via other components
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
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
        targetDistance: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.race_countdown_widget)

        if (raceDateStr.isEmpty()) {
            views.setTextViewText(R.id.widget_days_count, "--")
            views.setTextViewText(R.id.widget_days_label, "Chưa cài đặt giáo án")
            views.setTextViewText(R.id.widget_target, "Nhấp để thiết lập")
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

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
