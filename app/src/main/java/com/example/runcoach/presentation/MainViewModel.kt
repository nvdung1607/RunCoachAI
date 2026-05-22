package com.example.runcoach.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.runcoach.RunCoachApplication
import com.example.runcoach.data.local.db.WorkoutDao
import com.example.runcoach.data.local.db.WorkoutEntity
import com.example.runcoach.data.local.preferences.UserPreferences
import com.example.runcoach.data.local.preferences.UserPreferencesRepository
import com.example.runcoach.data.worker.SyncWorker
import com.example.runcoach.domain.plan.FitnessLevel
import com.example.runcoach.domain.plan.PlanGenerator
import com.example.runcoach.domain.plan.VdotCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val workoutDao: WorkoutDao,
    application: Application
) : AndroidViewModel(application) {

    val userPreferences: StateFlow<UserPreferences> = prefsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences("", "BEGINNER", 0f, false, 480, 420, 510, "SYSTEM", true, 6, 0, 21, 3, false, "MALE", 25, "SEDENTARY")
        )

    val workouts: StateFlow<List<WorkoutEntity>> = workoutDao.getAllWorkoutsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayWorkout: StateFlow<WorkoutEntity?> = workoutDao.getWorkoutFlowByDate(LocalDate.now().toString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveOnboarding(raceDate: String, fitnessLevel: String, targetDistance: Int, maxSessions: Int, gender: String, age: Int, activityLevel: String) {
        com.example.runcoach.utils.AppLogger.d("User saved onboarding: raceDate=$raceDate, level=$fitnessLevel, target=${targetDistance}km, sessions=$maxSessions")
        viewModelScope.launch {
            prefsRepository.saveOnboardingPreferences(raceDate, fitnessLevel, targetDistance, maxSessions, gender, age, activityLevel)
        }
    }

    fun completeTestRun(timeSeconds: Double) {
        com.example.runcoach.utils.AppLogger.i("User completed test run: time=$timeSeconds seconds")
        viewModelScope.launch {
            val vdot = VdotCalculator.calculateVdotFor3k(timeSeconds)
            val paceZones = VdotCalculator.calculatePaceZones(vdot)

            prefsRepository.saveFitnessProfile(
                vdotScore = vdot.toFloat(),
                easyPaceSec = paceZones.easyPaceSec,
                tempoPaceSec = paceZones.tempoPaceSec,
                longPaceSec = paceZones.longPaceSec
            )

            workoutDao.clearAllWorkouts()

            val prefs = userPreferences.value
            val level = when (prefs.fitnessLevel) {
                "INTERMEDIATE" -> FitnessLevel.INTERMEDIATE
                "ADVANCED" -> FitnessLevel.ADVANCED
                else -> FitnessLevel.BEGINNER
            }

            val startDate = LocalDate.now()
            val raceDateParsed = LocalDate.parse(prefs.raceDate)

            val generatedList = PlanGenerator.generatePlan(
                startDate = startDate,
                raceDate = raceDateParsed,
                vdotScore = vdot,
                level = level,
                targetDistance = prefs.targetDistance,
                maxSessionsPerWeek = prefs.maxSessionsPerWeek,
                age = prefs.age,
                gender = prefs.gender
            )

            workoutDao.insertAll(generatedList)

            val notifPrefs = userPreferences.value
            com.example.runcoach.presentation.receiver.WorkoutReminderReceiver.scheduleDailyAlarm(
                getApplication(),
                notifPrefs.notificationHour,
                notifPrefs.notificationMinute
            )
        }
    }

    fun markWorkoutCompletedManually(date: String, distanceKm: Double, durationMin: Double) {
        viewModelScope.launch {
            val workout = workoutDao.getWorkoutByDate(date)
            if (workout != null) {
                val updated = workout.copy(
                    isCompleted = true,
                    actualDistanceKm = distanceKm,
                    actualDurationMin = durationMin,
                    completedDate = LocalDate.now().toString(),
                    syncSource = "MANUAL"
                )
                workoutDao.update(updated)
            }
        }
    }

    fun updateWorkoutManualStats(date: String, distanceKm: Double, durationMin: Double, isCompleted: Boolean) {
        com.example.runcoach.utils.AppLogger.i("Updating manual workout stats for date=$date: distance=$distanceKm, duration=$durationMin, completed=$isCompleted")
        viewModelScope.launch {
            val workout = workoutDao.getWorkoutByDate(date)
            if (workout != null) {
                val updated = workout.copy(
                    isCompleted = isCompleted,
                    actualDistanceKm = if (isCompleted) distanceKm else 0.0,
                    actualDurationMin = if (isCompleted) durationMin else 0.0,
                    completedDate = if (isCompleted) LocalDate.now().toString() else null,
                    isSkipped = if (isCompleted) false else workout.isSkipped,
                    syncSource = if (isCompleted) "MANUAL" else null
                )
                workoutDao.update(updated)
            }
        }
    }

    /**
     * Rescheduling (Option B): Mark original as skipped, create a copy on the new date.
     * Returns: 0 = success, 1 = target date already has active workout (conflicts), -1 = original not found
     */
    fun rescheduleWorkout(
        originalDate: String,
        newDate: String,
        onResult: (result: Int, conflictDescription: String?) -> Unit
    ) {
        com.example.runcoach.utils.AppLogger.i("Rescheduling workout from $originalDate to $newDate")
        viewModelScope.launch {
            val original = workoutDao.getWorkoutByDate(originalDate)
            if (original == null) {
                onResult(-1, null)
                return@launch
            }

            // Check for conflicts at the new date
            val existingAtNew = workoutDao.getWorkoutByDate(newDate)
            val hasConflict = existingAtNew != null &&
                existingAtNew.type !in listOf("REST", "CT") &&
                !existingAtNew.isSkipped

            // Mark original as skipped
            workoutDao.update(original.copy(isSkipped = true))

            // Create rescheduled workout at new date
            val rescheduled = original.copy(
                date = newDate,
                isSkipped = false,
                rescheduledFromDate = originalDate,
                description = "${original.description} [Dời từ $originalDate]",
                isCompleted = false
            )

            // If conflict exists, insert alongside (both will show)
            workoutDao.insert(rescheduled)

            onResult(if (hasConflict) 1 else 0, existingAtNew?.description)
        }
    }

    fun triggerSync() {
        com.example.runcoach.utils.AppLogger.d("Manual Health Connect sync triggered by user")
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    fun syncRecentWorkouts(onResult: (String) -> Unit) {
        com.example.runcoach.utils.AppLogger.d("Starting syncRecentWorkouts for last 3 days")
        viewModelScope.launch {
            try {
                val app = getApplication<RunCoachApplication>()
                val healthConnectManager = com.example.runcoach.data.health.HealthConnectManager(app)
                
                if (!healthConnectManager.hasPermissions()) {
                    onResult("Chưa cấp quyền Health Connect.")
                    return@launch
                }

                val zoneId = java.time.ZoneId.systemDefault()
                val today = java.time.LocalDate.now()
                val windowStart = today.minusDays(2)
                
                val startInstant = java.time.ZonedDateTime.of(windowStart.atStartOfDay(), zoneId).toInstant()
                val endInstant = java.time.ZonedDateTime.of(today.plusDays(1).atStartOfDay(), zoneId).toInstant()

                val allSessions = healthConnectManager.getRunningSessions(startInstant, endInstant)
                
                if (allSessions.isEmpty()) {
                    onResult("Không tìm thấy dữ liệu chạy bộ nào trong 3 ngày gần nhất trên Health Connect.")
                    return@launch
                }

                // Group sessions by date
                val sessionsByDate = allSessions.groupBy { session ->
                    session.startTime.atZone(zoneId).toLocalDate()
                }
                
                val runningTypes = setOf("EASY", "LONG", "TEMPO", "RACE", "INTERVAL", "REPETITION", "RECOVERY")
                var syncedCount = 0
                var totalSyncedKm = 0.0

                for ((sessionDate, sessions) in sessionsByDate) {
                    val totalDistance = sessions.sumOf { it.distanceKm }
                    val totalDuration = sessions.sumOf { it.durationMinutes }
                    
                    if (totalDistance < 0.1) continue

                    // Find matching uncompleted workout within +-1 day
                    val candidateDates = listOf(
                        sessionDate.toString(),
                        sessionDate.minusDays(1).toString(),
                        sessionDate.plusDays(1).toString()
                    )
                    
                    for (dateStr in candidateDates) {
                        val workout = workoutDao.getWorkoutByDate(dateStr)
                        if (workout != null && !workout.isCompleted && workout.type in runningTypes) {
                            val updated = workout.copy(
                                isCompleted = true,
                                actualDistanceKm = totalDistance,
                                actualDurationMin = totalDuration,
                                completedDate = sessions.first().startTime.toString(),
                                syncSource = "HEALTH_CONNECT"
                            )
                            workoutDao.update(updated)
                            syncedCount++
                            totalSyncedKm += totalDistance
                            com.example.runcoach.utils.AppLogger.i(
                                "syncRecentWorkouts: Synced ${totalDistance}km from $sessionDate to workout on $dateStr"
                            )
                            break // Found a match for this session, move to next
                        }
                    }
                }

                if (syncedCount > 0) {
                    onResult("Đã đồng bộ thành công $syncedCount buổi tập! (Tổng: ${"%,.1f".format(totalSyncedKm)} km)")
                } else {
                    onResult("Tìm thấy ${allSessions.size} buổi chạy nhưng không khớp với bài tập nào trong giáo án.")
                }

            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("syncRecentWorkouts crashed", e)
                onResult("Đã xảy ra lỗi khi đồng bộ: ${e.message}")
            }
        }
    }

    // Backward compat alias
    fun syncTodayWorkout(onResult: (String) -> Unit) = syncRecentWorkouts(onResult)

    fun upsertWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.insert(workout)
        }
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.delete(workout)
        }
    }

    fun exportPlanToCsv(context: android.content.Context, onResult: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val list = workouts.value
                val csvContent = com.example.runcoach.domain.plan.PlanExporter.exportToCsv(list)
                
                val cacheDir = java.io.File(context.cacheDir, "exports")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                
                val file = java.io.File(cacheDir, "RunCoach_Plan_${System.currentTimeMillis()}.csv")
                file.writeText(csvContent)
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.runcoach.fileprovider",
                    file
                )
                onResult(uri)
            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("exportPlanToCsv failed", e)
                onResult(null)
            }
        }
    }

    fun exportPlanToPdf(context: android.content.Context, onResult: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val list = workouts.value
                
                val cacheDir = java.io.File(context.cacheDir, "exports")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                
                val file = java.io.File(cacheDir, "RunCoach_Plan_${System.currentTimeMillis()}.pdf")
                file.outputStream().use { os ->
                    com.example.runcoach.domain.plan.PlanExporter.exportToPdf(list, os)
                }
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.runcoach.fileprovider",
                    file
                )
                onResult(uri)
            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("exportPlanToPdf failed", e)
                onResult(null)
            }
        }
    }

    fun resetApp(onComplete: () -> Unit = {}) {
        com.example.runcoach.utils.AppLogger.w("Resetting the application database and preferences!")
        viewModelScope.launch {
            prefsRepository.clear()
            workoutDao.clearAllWorkouts()
            onComplete()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setNotificationsEnabled(enabled)
            val prefs = userPreferences.value
            if (enabled) {
                com.example.runcoach.presentation.receiver.WorkoutReminderReceiver.scheduleDailyAlarm(
                    getApplication(), prefs.notificationHour, prefs.notificationMinute
                )
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepository.setNotificationTime(hour, minute)
            val prefs = userPreferences.value
            if (prefs.isNotificationEnabled) {
                com.example.runcoach.presentation.receiver.WorkoutReminderReceiver.scheduleDailyAlarm(
                    getApplication(), hour, minute
                )
            }
        }
    }

    fun completePermissionSetup() {
        viewModelScope.launch {
            prefsRepository.savePermissionSetupCompleted()
        }
    }
}

class MainViewModelFactory(
    private val prefsRepository: UserPreferencesRepository,
    private val workoutDao: WorkoutDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(prefsRepository, workoutDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
