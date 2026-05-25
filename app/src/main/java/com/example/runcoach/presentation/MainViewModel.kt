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
import android.net.Uri
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ProposedSync(
    val exactMatches: List<ExactMatch>,
    val shifts: List<ProposedShift>
)

data class ExactMatch(
    val workout: WorkoutEntity,
    val actualDistanceKm: Double,
    val actualDurationMin: Double,
    val actualStartTime: String
)

data class ProposedShift(
    val workout: WorkoutEntity,
    val originalDate: String,
    val newDate: String,
    val workoutOnNewDate: WorkoutEntity?,
    val actualDistanceKm: Double,
    val actualDurationMin: Double,
    val actualStartTime: String
)

class MainViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val workoutDao: WorkoutDao,
    application: Application
) : AndroidViewModel(application) {

    val userPreferences: StateFlow<UserPreferences> = prefsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences("", "BEGINNER", 0f, false, 480, 420, 510, "SYSTEM", true, 6, 0, 21, 3, false, "MALE", 25, "SEDENTARY", false)
        )

    val workouts: StateFlow<List<WorkoutEntity>> = workoutDao.getAllWorkoutsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val todayWorkout: StateFlow<WorkoutEntity?> = workoutDao.getWorkoutFlowByDate(LocalDate.now().toString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        viewModelScope.launch {
            launch {
                workouts.collect {
                    updateWidget()
                }
            }
            launch {
                userPreferences.collect {
                    updateWidget()
                }
            }
        }
    }

    private fun updateWidget() {
        val intent = android.content.Intent(getApplication<Application>(), com.example.runcoach.presentation.receiver.RaceCountdownWidget::class.java).apply {
            action = "com.example.runcoach.ACTION_REFRESH_WIDGET"
        }
        getApplication<Application>().sendBroadcast(intent)
        com.example.runcoach.utils.AppLogger.d("Sent explicit ACTION_REFRESH_WIDGET broadcast to update app widget.")
    }

    fun saveOnboarding(raceDate: String, fitnessLevel: String, targetDistance: Int, maxSessions: Int, gender: String, age: Int, activityLevel: String) {
        com.example.runcoach.utils.AppLogger.d("User saved onboarding: raceDate=$raceDate, level=$fitnessLevel, target=${targetDistance}km, sessions=$maxSessions")
        viewModelScope.launch {
            prefsRepository.saveOnboardingPreferences(raceDate, fitnessLevel, targetDistance, maxSessions, gender, age, activityLevel)
            updateWidget()
        }
    }

    fun completeTestRun(timeSeconds: Double, onSuccess: () -> Unit = {}) {
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
            onSuccess()
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
                updateWidget()
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
                updateWidget()
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
            
            updateWidget()

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
                        val workout = workoutDao.getWorkoutByDate(dateStr)
                        if (workout != null && !workout.isCompleted && workout.type in runningTypes) {
                            if (dateStr != sessionDate.toString()) {
                                // We need to shift this planned workout from dateStr to sessionDate!
                                val workoutOnSessionDate = workoutDao.getWorkoutByDate(sessionDate.toString())
                                
                                val updatedMatchingWorkout = workout.copy(
                                    date = sessionDate.toString(),
                                    weekNumber = workoutOnSessionDate?.weekNumber ?: workout.weekNumber,
                                    isCompleted = true,
                                    actualDistanceKm = totalDistance,
                                    actualDurationMin = totalDuration,
                                    completedDate = sessions.first().startTime.toString(),
                                    syncSource = "HEALTH_CONNECT"
                                )
                                
                                val updatedOtherWorkout = if (workoutOnSessionDate != null) {
                                    workoutOnSessionDate.copy(
                                        date = workout.date,
                                        weekNumber = workout.weekNumber
                                    )
                                } else {
                                    WorkoutEntity(
                                        date = workout.date,
                                        weekNumber = workout.weekNumber,
                                        type = "REST",
                                        targetDistanceKm = 0.0,
                                        targetPaceSec = 0,
                                        description = "Nghỉ ngơi hoàn toàn",
                                        instructions = "Cơ thể phục hồi và phát triển trong những ngày nghỉ.",
                                        isCompleted = false
                                    )
                                }
                                
                                // Delete both old keys first
                                workoutDao.delete(workout)
                                if (workoutOnSessionDate != null) {
                                    workoutDao.delete(workoutOnSessionDate)
                                }
                                
                                // Insert updated ones
                                workoutDao.insert(updatedMatchingWorkout)
                                if (updatedOtherWorkout != null) {
                                    workoutDao.insert(updatedOtherWorkout)
                                }
                                
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
                                val sessionDateFormatted = sessionDate.format(formatter)
                                val originalDateFormatted = java.time.LocalDate.parse(workout.date).format(formatter)
                                shifts.add("Chuyển bài tập ngày $originalDateFormatted sang ngày $sessionDateFormatted (${workout.description})")
                                
                                com.example.runcoach.utils.AppLogger.i(
                                    "syncRecentWorkouts: Shifted and completed workout from $dateStr to $sessionDate"
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
                                workoutDao.update(updated)
                                com.example.runcoach.utils.AppLogger.i(
                                    "syncRecentWorkouts: Synced ${totalDistance}km on exact date $sessionDate"
                                )
                            }
                            syncedCount++
                            totalSyncedKm += totalDistance
                            break // Found a match for this session, move to next date
                        }
                    }
                }

                if (syncedCount > 0) {
                    var resultMsg = "Đã đồng bộ thành công $syncedCount buổi tập! (Tổng: ${"%,.1f".format(totalSyncedKm)} km)"
                    if (shifts.isNotEmpty()) {
                        resultMsg += "\n\n💡 Hệ thống đã tự động điều chỉnh giáo án cho khớp với thực tế chạy:\n" + shifts.joinToString("\n") { "• $it" }
                    }
                    onResult(resultMsg)
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

    fun checkSyncProposed(onResult: (ProposedSync?, String?) -> Unit) {
        com.example.runcoach.utils.AppLogger.d("Checking proposed Health Connect sync")
        viewModelScope.launch {
            try {
                val app = getApplication<RunCoachApplication>()
                val healthConnectManager = com.example.runcoach.data.health.HealthConnectManager(app)
                
                if (!healthConnectManager.hasPermissions()) {
                    onResult(null, "Chưa cấp quyền Health Connect.")
                    return@launch
                }

                val zoneId = java.time.ZoneId.systemDefault()
                val today = java.time.LocalDate.now()
                val windowStart = today.minusDays(2)
                
                val startInstant = java.time.ZonedDateTime.of(windowStart.atStartOfDay(), zoneId).toInstant()
                val endInstant = java.time.ZonedDateTime.of(today.plusDays(1).atStartOfDay(), zoneId).toInstant()

                val allSessions = healthConnectManager.getRunningSessions(startInstant, endInstant)
                
                if (allSessions.isEmpty()) {
                    onResult(null, "Không tìm thấy dữ liệu chạy bộ nào trong 3 ngày gần nhất trên Health Connect.")
                    return@launch
                }

                // Group sessions by date
                val sessionsByDate = allSessions.groupBy { session ->
                    session.startTime.atZone(zoneId).toLocalDate()
                }
                
                val runningTypes = setOf("EASY", "LONG", "TEMPO", "RACE", "INTERVAL", "REPETITION", "RECOVERY")
                val exactMatches = mutableListOf<ExactMatch>()
                val shifts = mutableListOf<ProposedShift>()

                for ((sessionDate, sessions) in sessionsByDate) {
                    val totalDistance = sessions.sumOf { it.distanceKm }
                    val totalDuration = sessions.sumOf { it.durationMinutes }
                    
                    if (totalDistance < 0.5) continue // Coerce at least 0.5km

                    // Find matching uncompleted workout within +-1 day
                    val candidateDates = listOf(
                        sessionDate.toString(),
                        sessionDate.minusDays(1).toString(),
                        sessionDate.plusDays(1).toString()
                    )
                    
                    for (dateStr in candidateDates) {
                        val workout = workoutDao.getWorkoutByDate(dateStr)
                        if (workout != null && !workout.isCompleted && workout.type in runningTypes) {
                            if (dateStr != sessionDate.toString()) {
                                // Mismatch: shift required
                                val workoutOnSessionDate = workoutDao.getWorkoutByDate(sessionDate.toString())
                                shifts.add(
                                    ProposedShift(
                                        workout = workout,
                                        originalDate = dateStr,
                                        newDate = sessionDate.toString(),
                                        workoutOnNewDate = workoutOnSessionDate,
                                        actualDistanceKm = totalDistance,
                                        actualDurationMin = totalDuration,
                                        actualStartTime = sessions.first().startTime.toString()
                                    )
                                )
                            } else {
                                // Match on exact date
                                exactMatches.add(
                                    ExactMatch(
                                        workout = workout,
                                        actualDistanceKm = totalDistance,
                                        actualDurationMin = totalDuration,
                                        actualStartTime = sessions.first().startTime.toString()
                                    )
                                )
                            }
                            break // Found match for this session date
                        }
                    }
                }

                if (exactMatches.isEmpty() && shifts.isEmpty()) {
                    onResult(null, "Tìm thấy ${allSessions.size} buổi chạy nhưng không khớp với bài tập chưa hoàn thành nào trong giáo án.")
                } else {
                    onResult(ProposedSync(exactMatches, shifts), null)
                }

            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("checkSyncProposed crashed", e)
                onResult(null, "Đã xảy ra lỗi khi kiểm tra đồng bộ: ${e.message}")
            }
        }
    }

    fun applySync(proposedSync: ProposedSync, onComplete: (String) -> Unit) {
        com.example.runcoach.utils.AppLogger.d("Applying Proposed Sync: exact=${proposedSync.exactMatches.size}, shifts=${proposedSync.shifts.size}")
        viewModelScope.launch {
            try {
                var syncedCount = 0
                var totalSyncedKm = 0.0
                val shiftDescriptions = mutableListOf<String>()

                // 1. Apply exact matches
                for (match in proposedSync.exactMatches) {
                    val updated = match.workout.copy(
                        isCompleted = true,
                        actualDistanceKm = match.actualDistanceKm,
                        actualDurationMin = match.actualDurationMin,
                        completedDate = match.actualStartTime,
                        syncSource = "HEALTH_CONNECT"
                    )
                    workoutDao.update(updated)
                    syncedCount++
                    totalSyncedKm += match.actualDistanceKm
                }

                // 2. Apply shifts & swaps
                for (shift in proposedSync.shifts) {
                    val updatedMatchingWorkout = shift.workout.copy(
                        date = shift.newDate,
                        weekNumber = shift.workoutOnNewDate?.weekNumber ?: shift.workout.weekNumber,
                        isCompleted = true,
                        actualDistanceKm = shift.actualDistanceKm,
                        actualDurationMin = shift.actualDurationMin,
                        completedDate = shift.actualStartTime,
                        syncSource = "HEALTH_CONNECT"
                    )
                    
                    val updatedOtherWorkout = if (shift.workoutOnNewDate != null) {
                        shift.workoutOnNewDate.copy(
                            date = shift.originalDate,
                            weekNumber = shift.workout.weekNumber
                        )
                    } else {
                        WorkoutEntity(
                            date = shift.originalDate,
                            weekNumber = shift.workout.weekNumber,
                            type = "REST",
                            targetDistanceKm = 0.0,
                            targetPaceSec = 0,
                            description = "Nghỉ ngơi hoàn toàn",
                            instructions = "Cơ thể phục hồi và phát triển trong những ngày nghỉ.",
                            isCompleted = false
                        )
                    }
                    
                    // Delete old records first to prevent primary key constraint errors
                    workoutDao.delete(shift.workout)
                    if (shift.workoutOnNewDate != null) {
                        workoutDao.delete(shift.workoutOnNewDate)
                    }
                    
                    // Insert updated records
                    workoutDao.insert(updatedMatchingWorkout)
                    if (updatedOtherWorkout != null) {
                        workoutDao.insert(updatedOtherWorkout)
                    }

                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
                    val sessionDateFormatted = java.time.LocalDate.parse(shift.newDate).format(formatter)
                    val originalDateFormatted = java.time.LocalDate.parse(shift.originalDate).format(formatter)
                    shiftDescriptions.add("Chuyển bài tập ngày $originalDateFormatted sang ngày $sessionDateFormatted (${shift.workout.description})")
                    
                    syncedCount++
                    totalSyncedKm += shift.actualDistanceKm
                }

                updateWidget()

                var resultMsg = "Đã đồng bộ thành công $syncedCount buổi tập! (Tổng: ${"%,.1f".format(totalSyncedKm)} km)"
                if (shiftDescriptions.isNotEmpty()) {
                    resultMsg += "\n\n💡 Lịch tập luyện đã được điều chỉnh:\n" + shiftDescriptions.joinToString("\n") { "• $it" }
                }
                onComplete(resultMsg)

            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("applySync crashed", e)
                onComplete("Đã xảy ra lỗi khi lưu đồng bộ: ${e.message}")
            }
        }
    }

    fun upsertWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.insert(workout)
            updateWidget()
        }
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.delete(workout)
            updateWidget()
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

    fun importPlanFromCsv(
        context: android.content.Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    onError("Không thể mở file CSV.")
                    return@launch
                }

                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                val lines = reader.use { it.readLines() }

                if (lines.isEmpty()) {
                    onError("File CSV trống.")
                    return@launch
                }

                val expectedHeader = "Ngày,Tuần,Loại bài tập,Cự ly mục tiêu (km),Pace mục tiêu (phút/km),Mô tả,Hướng dẫn,Hoàn thành,Cự ly thực tế (km),Thời gian thực tế (phút),Ghi chú"
                var header = lines.first().trim()
                if (header.startsWith("\uFEFF")) {
                    header = header.substring(1)
                }

                if (header != expectedHeader) {
                    onError("Định dạng file CSV không đúng.\nTiêu đề cột thực tế: $header\nTiêu đề cột yêu cầu: $expectedHeader")
                    return@launch
                }

                val workoutsToInsert = mutableListOf<WorkoutEntity>()
                val validTypes = setOf("EASY", "LONG", "TEMPO", "RACE", "INTERVAL", "REPETITION", "RECOVERY", "REST", "CT")
                val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue

                    val fields = parseCsvLine(line)
                    if (fields.size < 11) {
                        onError("Dòng ${i + 1} thiếu thông tin (yêu cầu 11 cột, hiện có ${fields.size} cột). Dòng: $line")
                        return@launch
                    }

                    val dateStr = fields[0].trim()
                    val weekStr = fields[1].trim()
                    val typeStr = fields[2].trim().uppercase()
                    val targetDistStr = fields[3].trim()
                    val targetPaceStr = fields[4].trim()
                    val descStr = fields[5].trim()
                    val instructionsStr = fields[6].trim()
                    val completedStr = fields[7].trim().uppercase()
                    val actualDistStr = fields[8].trim()
                    val actualDurStr = fields[9].trim()
                    val notesStr = fields[10].trim()

                    // Validate Date (YYYY-MM-DD)
                    try {
                        java.time.LocalDate.parse(dateStr, dateFormatter)
                    } catch (e: Exception) {
                        onError("Dòng ${i + 1}: Ngày '$dateStr' không hợp lệ (định dạng đúng: YYYY-MM-DD).")
                        return@launch
                    }

                    // Validate Week Number
                    val weekNum = weekStr.toIntOrNull()
                    if (weekNum == null || weekNum <= 0) {
                        onError("Dòng ${i + 1}: Số tuần '$weekStr' không hợp lệ (phải là số nguyên dương).")
                        return@launch
                    }

                    // Validate Type
                    if (typeStr !in validTypes) {
                        onError("Dòng ${i + 1}: Loại bài tập '$typeStr' không hợp lệ. Chỉ chấp nhận các loại: ${validTypes.joinToString(", ")}")
                        return@launch
                    }

                    // Validate Target Distance
                    val targetDist = if (targetDistStr.isEmpty()) 0.0 else targetDistStr.toDoubleOrNull()
                    if (targetDist == null || targetDist < 0) {
                        onError("Dòng ${i + 1}: Cự ly mục tiêu '$targetDistStr' không hợp lệ (phải là số không âm).")
                        return@launch
                    }

                    // Validate Target Pace (can be MM:SS or empty)
                    var targetPaceSec = 0
                    if (targetPaceStr.isNotEmpty() && targetPaceStr != "-") {
                        val paceParts = targetPaceStr.split(":")
                        if (paceParts.size == 2) {
                            val m = paceParts[0].toIntOrNull()
                            val s = paceParts[1].toIntOrNull()
                            if (m != null && s != null && m >= 0 && s >= 0 && s < 60) {
                                targetPaceSec = m * 60 + s
                            } else {
                                onError("Dòng ${i + 1}: Pace mục tiêu '$targetPaceStr' không hợp lệ (định dạng đúng: MM:SS).")
                                return@launch
                            }
                        } else {
                            onError("Dòng ${i + 1}: Pace mục tiêu '$targetPaceStr' không hợp lệ (định dạng đúng: MM:SS).")
                            return@launch
                        }
                    }

                    // Validate Completion
                    val isCompleted = when (completedStr) {
                        "CÓ", "YES", "TRUE", "1" -> true
                        "KHÔNG", "NO", "FALSE", "0", "" -> false
                        else -> {
                            onError("Dòng ${i + 1}: Trạng thái hoàn thành '$completedStr' không hợp lệ (chấp nhận: 'Có' hoặc 'Không').")
                            return@launch
                        }
                    }

                    // Validate Actual Distance
                    val actualDist = if (actualDistStr.isEmpty()) 0.0 else actualDistStr.toDoubleOrNull()
                    if (actualDist == null || actualDist < 0) {
                        onError("Dòng ${i + 1}: Cự ly thực tế '$actualDistStr' không hợp lệ (phải là số không âm).")
                        return@launch
                    }

                    // Validate Actual Duration
                    val actualDur = if (actualDurStr.isEmpty()) 0.0 else actualDurStr.toDoubleOrNull()
                    if (actualDur == null || actualDur < 0) {
                        onError("Dòng ${i + 1}: Thời gian thực tế '$actualDurStr' không hợp lệ (phải là số không âm).")
                        return@launch
                    }

                    val workout = WorkoutEntity(
                        date = dateStr,
                        weekNumber = weekNum,
                        type = typeStr,
                        targetDistanceKm = targetDist,
                        targetPaceSec = targetPaceSec,
                        description = descStr,
                        instructions = instructionsStr,
                        isCompleted = isCompleted,
                        actualDistanceKm = actualDist,
                        actualDurationMin = actualDur,
                        notes = notesStr,
                        isCustom = true
                    )
                    workoutsToInsert.add(workout)
                }

                // Clear and insert
                workoutDao.clearAllWorkouts()
                workoutDao.insertAll(workoutsToInsert)

                onSuccess("Nhập giáo án thành công! Đã thêm ${workoutsToInsert.size} bài tập.")

            } catch (e: Exception) {
                com.example.runcoach.utils.AppLogger.e("importPlanFromCsv failed", e)
                onError("Lỗi khi đọc file CSV: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    currentField.append('"')
                    i += 2
                    continue
                }
                inQuotes = !inQuotes
                i++
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString())
                currentField = StringBuilder()
                i++
            } else {
                currentField.append(c)
                i++
            }
        }
        result.add(currentField.toString())
        return result
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

    fun swapWorkouts(
        workoutA: WorkoutEntity,
        workoutB: WorkoutEntity,
        applyToSubsequentWeeks: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val localDateA = LocalDate.parse(workoutA.date)
            val localDateB = LocalDate.parse(workoutB.date)
            val dayOfWeekA = localDateA.dayOfWeek
            val dayOfWeekB = localDateB.dayOfWeek

            if (!applyToSubsequentWeeks) {
                val updatedA = workoutA.copy(
                    date = workoutB.date,
                    weekNumber = workoutB.weekNumber
                )
                val updatedB = workoutB.copy(
                    date = workoutA.date,
                    weekNumber = workoutA.weekNumber
                )
                workoutDao.insertAll(listOf(updatedA, updatedB))
            } else {
                val startWeek = minOf(workoutA.weekNumber, workoutB.weekNumber)
                val allWorkouts = workoutDao.getWorkoutsFromWeekDirect(startWeek)
                val workoutsByWeek = allWorkouts.groupBy { it.weekNumber }
                val updatedList = mutableListOf<WorkoutEntity>()

                for ((week, weekWorkouts) in workoutsByWeek) {
                    val wA = weekWorkouts.find { LocalDate.parse(it.date).dayOfWeek == dayOfWeekA }
                    val wB = weekWorkouts.find { LocalDate.parse(it.date).dayOfWeek == dayOfWeekB }

                    if (wA != null && wB != null) {
                        val updatedWA = wA.copy(
                            type = wB.type,
                            targetDistanceKm = wB.targetDistanceKm,
                            targetPaceSec = wB.targetPaceSec,
                            description = wB.description,
                            instructions = wB.instructions,
                            isCompleted = wB.isCompleted,
                            isSkipped = wB.isSkipped,
                            rescheduledFromDate = wB.rescheduledFromDate,
                            actualDistanceKm = wB.actualDistanceKm,
                            actualDurationMin = wB.actualDurationMin,
                            completedDate = wB.completedDate,
                            syncSource = wB.syncSource,
                            notes = wB.notes,
                            isCustom = wB.isCustom
                        )
                        val updatedWB = wB.copy(
                            type = wA.type,
                            targetDistanceKm = wA.targetDistanceKm,
                            targetPaceSec = wA.targetPaceSec,
                            description = wA.description,
                            instructions = wA.instructions,
                            isCompleted = wA.isCompleted,
                            isSkipped = wA.isSkipped,
                            rescheduledFromDate = wA.rescheduledFromDate,
                            actualDistanceKm = wA.actualDistanceKm,
                            actualDurationMin = wA.actualDurationMin,
                            completedDate = wA.completedDate,
                            syncSource = wA.syncSource,
                            notes = wA.notes,
                            isCustom = wA.isCustom
                        )
                        updatedList.add(updatedWA)
                        updatedList.add(updatedWB)
                    }
                }
                if (updatedList.isNotEmpty()) {
                    workoutDao.insertAll(updatedList)
                }
            }
            updateWidget()
            onComplete()
        }
    }

    fun regeneratePlan(
        raceDate: String,
        targetDistance: Int,
        gender: String,
        age: Int,
        timeSeconds: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val vdot = VdotCalculator.calculateVdotFor3k(timeSeconds)
            val paceZones = VdotCalculator.calculatePaceZones(vdot)
            val currentPrefs = userPreferences.value

            prefsRepository.saveOnboardingPreferences(
                raceDate = raceDate,
                fitnessLevel = currentPrefs.fitnessLevel,
                targetDistance = targetDistance,
                maxSessions = currentPrefs.maxSessionsPerWeek,
                gender = gender,
                age = age,
                activityLevel = currentPrefs.activityLevel
            )

            prefsRepository.saveFitnessProfile(
                vdotScore = vdot.toFloat(),
                easyPaceSec = paceZones.easyPaceSec,
                tempoPaceSec = paceZones.tempoPaceSec,
                longPaceSec = paceZones.longPaceSec
            )

            workoutDao.clearAllWorkouts()

            val startDate = LocalDate.now()
            val raceDateParsed = LocalDate.parse(raceDate)
            val level = when (currentPrefs.fitnessLevel) {
                "INTERMEDIATE" -> FitnessLevel.INTERMEDIATE
                "ADVANCED" -> FitnessLevel.ADVANCED
                else -> FitnessLevel.BEGINNER
            }

            val generatedList = PlanGenerator.generatePlan(
                startDate = startDate,
                raceDate = raceDateParsed,
                vdotScore = vdot,
                level = level,
                targetDistance = targetDistance,
                maxSessionsPerWeek = currentPrefs.maxSessionsPerWeek,
                age = age,
                gender = gender
            )

            workoutDao.insertAll(generatedList)

            val notifPrefs = userPreferences.value
            com.example.runcoach.presentation.receiver.WorkoutReminderReceiver.scheduleDailyAlarm(
                getApplication(),
                notifPrefs.notificationHour,
                notifPrefs.notificationMinute
            )

            updateWidget()
            onComplete()
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
