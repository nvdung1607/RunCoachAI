package com.example.runcoach.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
            initialValue = UserPreferences("", "BEGINNER", 0f, false, 480, 420, 510, "SYSTEM", true, 6, 0, 21, 3, false)
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

    fun saveOnboarding(raceDate: String, fitnessLevel: String, targetDistance: Int, maxSessions: Int) {
        viewModelScope.launch {
            prefsRepository.saveOnboardingPreferences(raceDate, fitnessLevel, targetDistance, maxSessions)
        }
    }

    fun completeTestRun(timeSeconds: Double) {
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
                maxSessionsPerWeek = prefs.maxSessionsPerWeek
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
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    fun resetApp(onComplete: () -> Unit = {}) {
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
