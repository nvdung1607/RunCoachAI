package com.example.runcoach.domain.plan

import com.example.runcoach.data.local.db.WorkoutEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class FitnessLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

data class FitnessConfig(
    val startDistance: Double,
    val peakDistance: Double,
    val maxStep: Double
)

enum class Phase {
    BASE, BUILD, PEAK, TAPER
}

enum class WorkoutRole {
    EASY_1, EASY_2, EASY_3, QUALITY, LONG, REST
}

object PlanGenerator {

    private fun getFitnessConfig(targetDistance: Int, level: FitnessLevel): FitnessConfig {
        return when (targetDistance) {
            5 -> when (level) {
                FitnessLevel.BEGINNER -> FitnessConfig(3.0, 6.0, 0.8)
                FitnessLevel.INTERMEDIATE -> FitnessConfig(4.0, 8.0, 1.0)
                FitnessLevel.ADVANCED -> FitnessConfig(5.0, 10.0, 1.2)
            }
            10 -> when (level) {
                FitnessLevel.BEGINNER -> FitnessConfig(5.0, 10.0, 1.0)
                FitnessLevel.INTERMEDIATE -> FitnessConfig(6.0, 12.0, 1.2)
                FitnessLevel.ADVANCED -> FitnessConfig(8.0, 14.0, 1.5)
            }
            42 -> when (level) {
                FitnessLevel.BEGINNER -> FitnessConfig(8.0, 24.0, 1.5)
                FitnessLevel.INTERMEDIATE -> FitnessConfig(12.0, 30.0, 2.0)
                FitnessLevel.ADVANCED -> FitnessConfig(16.0, 32.0, 2.5)
            }
            else -> when (level) { // 21k
                FitnessLevel.BEGINNER -> FitnessConfig(5.0, 14.0, 1.2)
                FitnessLevel.INTERMEDIATE -> FitnessConfig(8.0, 18.0, 1.5)
                FitnessLevel.ADVANCED -> FitnessConfig(10.0, 21.0, 2.0)
            }
        }
    }

    private fun getPhase(w: Int, baseWeeks: Int, buildWeeks: Int, peakWeeks: Int): Phase {
        return when {
            w <= baseWeeks -> Phase.BASE
            w <= baseWeeks + buildWeeks -> Phase.BUILD
            w <= baseWeeks + buildWeeks + peakWeeks -> Phase.PEAK
            else -> Phase.TAPER
        }
    }

    private fun getWorkoutSchedule(sessions: Int, longRunDay: DayOfWeek): Map<DayOfWeek, WorkoutRole> {
        val schedule = mutableMapOf<DayOfWeek, WorkoutRole>()
        DayOfWeek.values().forEach { schedule[it] = WorkoutRole.REST }
        
        schedule[longRunDay] = WorkoutRole.LONG
        val lrIndex = longRunDay.value
        
        fun getDay(offset: Int): DayOfWeek {
            val newIdx = (lrIndex - 1 + offset + 7) % 7
            return DayOfWeek.of(newIdx + 1)
        }

        when (sessions) {
            2 -> {
                schedule[getDay(-3)] = WorkoutRole.EASY_1
            }
            3 -> {
                schedule[getDay(-5)] = WorkoutRole.EASY_1
                schedule[getDay(-3)] = WorkoutRole.QUALITY
            }
            4 -> {
                schedule[getDay(-5)] = WorkoutRole.EASY_1
                schedule[getDay(-3)] = WorkoutRole.QUALITY
                schedule[getDay(-2)] = WorkoutRole.EASY_2
            }
            else -> { // 5 or more
                schedule[getDay(-6)] = WorkoutRole.EASY_3
                schedule[getDay(-5)] = WorkoutRole.EASY_1
                schedule[getDay(-3)] = WorkoutRole.QUALITY
                schedule[getDay(-2)] = WorkoutRole.EASY_2
            }
        }
        return schedule
    }

    fun generatePlan(
        startDate: LocalDate,
        raceDate: LocalDate,
        vdotScore: Double,
        level: FitnessLevel,
        targetDistance: Int = 21,
        maxSessionsPerWeek: Int = 3,
        preferredLongRunDay: DayOfWeek = DayOfWeek.SUNDAY
    ): List<WorkoutEntity> {
        val workouts = mutableListOf<WorkoutEntity>()
        val totalDays = ChronoUnit.DAYS.between(startDate, raceDate)
        val totalWeeks = (totalDays / 7.0).coerceAtLeast(4.0).toInt() + 1
        
        com.example.runcoach.utils.AppLogger.i("generatePlan: Start generating ${totalWeeks} weeks plan for ${targetDistance}km. Level=$level, MaxSessions=$maxSessionsPerWeek, VDOT=$vdotScore")

        val paceZones = VdotCalculator.calculatePaceZones(vdotScore)
        val isVeryBeginner = vdotScore < 25.0 && level == FitnessLevel.BEGINNER

        val raceDist = when (targetDistance) {
            5 -> 5.0
            10 -> 10.0
            42 -> 42.195
            else -> 21.0975
        }

        val racePace = when (targetDistance) {
            5 -> paceZones.intervalPaceSec
            10 -> paceZones.tempoPaceSec
            42 -> paceZones.marathonPaceSec
            else -> paceZones.tempoPaceSec
        }

        // Periodization phases
        val baseWeeks = maxOf(1, Math.round(totalWeeks * 0.3).toInt())
        val buildWeeks = maxOf(1, Math.round(totalWeeks * 0.4).toInt())
        val taperWeeks = if (totalWeeks > 8) 3 else if (totalWeeks > 4) 2 else 1
        val peakWeeks = maxOf(1, totalWeeks - baseWeeks - buildWeeks - taperWeeks)

        val config = getFitnessConfig(targetDistance, level)
        val longRunDistances = DoubleArray(totalWeeks + 1)
        val baseDist = DoubleArray(totalWeeks + 1)

        for (w in 1..totalWeeks) {
            if (w == totalWeeks) {
                longRunDistances[w] = raceDist
                continue
            }
            
            val phase = getPhase(w, baseWeeks, buildWeeks, peakWeeks)
            val isRecovery = w % 4 == 0 && phase != Phase.PEAK && phase != Phase.TAPER
            var dist = 0.0

            if (level == FitnessLevel.BEGINNER && w <= 3) {
                // Habit building weeks: start very easy
                dist = when (w) {
                    1 -> when (targetDistance) {
                        42 -> 6.0
                        21 -> 5.0
                        10 -> 4.0
                        else -> 3.0
                    }
                    2 -> when (targetDistance) {
                        42 -> 7.0
                        21 -> 6.0
                        10 -> 5.0
                        else -> 3.5
                    }
                    else -> when (targetDistance) {
                        42 -> 8.0
                        21 -> 7.0
                        10 -> 6.0
                        else -> 4.0
                    }
                }
                baseDist[w] = dist
            } else if (phase == Phase.TAPER) {
                val maxPreTaperDist = if (w > 1) {
                    (1 until w).map { baseDist[it] }.maxOrNull() ?: config.startDistance
                } else {
                    config.startDistance
                }
                val taperW = w - (baseWeeks + buildWeeks + peakWeeks)
                dist = if (taperWeeks == 3) {
                    if (taperW == 1) maxPreTaperDist * 0.80 else maxPreTaperDist * 0.60
                } else if (taperWeeks == 2) {
                    maxPreTaperDist * 0.70
                } else {
                    maxPreTaperDist * 0.60
                }
                baseDist[w] = dist
            } else {
                when (phase) {
                    Phase.BASE -> {
                        val progress = (w - 1).toDouble() / maxOf(1, baseWeeks)
                        val endBaseDist = config.startDistance + (config.peakDistance - config.startDistance) * 0.5
                        dist = config.startDistance + (endBaseDist - config.startDistance) * progress
                    }
                    Phase.BUILD -> {
                        val startBuildDist = config.startDistance + (config.peakDistance - config.startDistance) * 0.5
                        val progress = (w - baseWeeks - 1).toDouble() / maxOf(1, buildWeeks - 1)
                        dist = startBuildDist + (config.peakDistance - startBuildDist) * progress
                    }
                    Phase.PEAK -> {
                        dist = config.peakDistance
                    }
                    else -> {
                        dist = config.peakDistance
                    }
                }
                // Cap progression with maxStep relative to previous week's baseline
                if (w == 1) {
                    baseDist[1] = config.startDistance
                } else {
                    val prevBase = baseDist[w - 1]
                    val prev = if (prevBase > 0.0) prevBase else config.startDistance
                    baseDist[w] = minOf(dist, prev + config.maxStep)
                }
            }
            
            var finalDist = baseDist[w]
            if (isRecovery) finalDist *= 0.75
            longRunDistances[w] = (Math.round(finalDist * 10.0) / 10.0).coerceAtLeast(2.0)
        }

        val schedule = getWorkoutSchedule(maxSessionsPerWeek, preferredLongRunDay)
        val weeklyWorkoutCount = mutableMapOf<Int, Int>()
        var activeWorkoutCount = 0

        for (dayOffset in 0..totalDays) {
            val currentDate = startDate.plusDays(dayOffset)
            val startWeekMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeekMonday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekNumber = (ChronoUnit.WEEKS.between(startWeekMonday, currentWeekMonday).toInt() + 1).coerceAtMost(totalWeeks)

            val dayOfWeek = currentDate.dayOfWeek

            if (currentDate.isEqual(raceDate)) {
                val raceDistDesc = when (targetDistance) {
                    5 -> "5 km"
                    10 -> "10 km"
                    42 -> "42.195 km"
                    21 -> "21.0975 km"
                    else -> "$targetDistance km"
                }
                workouts.add(WorkoutEntity(
                    date = currentDate.toString(),
                    weekNumber = weekNumber,
                    type = "RACE",
                    targetDistanceKm = raceDist,
                    targetPaceSec = racePace,
                    description = "RACE DAY! Chinh phục cự ly $raceDistDesc",
                    instructions = "Chúc mừng bạn! Hôm nay là ngày tỏa sáng. Khởi động kỹ, giữ tốc độ ổn định ở nửa đầu và bứt phá ở nửa cuối.",
                    isCompleted = false
                ))
                continue
            }

            if (weekNumber == totalWeeks) {
                // Race Week tapering
                if (schedule[dayOfWeek] == WorkoutRole.EASY_1 || schedule[dayOfWeek] == WorkoutRole.QUALITY) {
                    val isFirst = schedule[dayOfWeek] == WorkoutRole.EASY_1
                    val dist = if (isFirst) 3.0 else 2.0
                    workouts.add(createEasyRun(currentDate, weekNumber, dist, paceZones.easyPaceSec, level, isVeryBeginner, "Chạy thả lỏng trước Race"))
                } else {
                    workouts.add(createRestWorkout(currentDate, weekNumber))
                }
                continue
            }

            var role = schedule[dayOfWeek] ?: WorkoutRole.REST
            
            // Force first workout to be today
            if (dayOffset == 0L && role == WorkoutRole.REST) {
                role = WorkoutRole.EASY_1
            }

            var isWorkout = role != WorkoutRole.REST
            if (isWorkout) {
                val currentWeekCount = weeklyWorkoutCount[weekNumber] ?: 0
                if (currentWeekCount >= maxSessionsPerWeek) {
                    role = WorkoutRole.REST
                    isWorkout = false
                } else {
                    weeklyWorkoutCount[weekNumber] = currentWeekCount + 1
                    activeWorkoutCount++
                }
            }

            val finalRole = if (isWorkout && activeWorkoutCount <= 3) {
                WorkoutRole.EASY_1
            } else {
                role
            }

            val phase = getPhase(weekNumber, baseWeeks, buildWeeks, peakWeeks)
            val isRecoveryWeek = weekNumber % 4 == 0 && phase != Phase.PEAK && phase != Phase.TAPER
            val lDist = longRunDistances[weekNumber]

            val workout = when (finalRole) {
                WorkoutRole.REST -> createRestWorkout(currentDate, weekNumber)
                WorkoutRole.LONG -> {
                    val hasMPace = phase == Phase.PEAK
                    val targetPace = if (hasMPace) paceZones.marathonPaceSec else paceZones.longPaceSec
                    val typeStr = if (hasMPace) "LONG (M-Pace)" else "LONG"
                    val desc = if (hasMPace) "Chạy dài Đạt đỉnh (Marathon Pace) (${lDist}km) 🏆" else "Chạy dài tích lũy sức bền (${lDist}km)"
                    val instr = if (level == FitnessLevel.BEGINNER) {
                        val baseInstructions = getRunWalkInstructions(level, weekNumber, isVeryBeginner)
                        val prefix = if (weekNumber <= 3) {
                            "Giai đoạn tạo thói quen chạy bộ. KHÔNG ĐẶT NẶNG QUÃNG ĐƯỜNG VÀ PACE. Hãy chạy thật chậm, đi bộ nghỉ ngơi thoải mái khi mệt. "
                        } else ""
                        "$prefix$baseInstructions Uống nước sau mỗi 3-5km."
                    } else {
                        if (hasMPace) "Mô phỏng ngày thi đấu. Giữ tốc độ mục tiêu (Marathon Pace) trong phần lớn quãng đường." else "Chạy ở tốc độ thoải mái, có thể trò chuyện. Uống nước sau mỗi 3-5km."
                    }
                    WorkoutEntity(
                        date = currentDate.toString(),
                        weekNumber = weekNumber,
                        type = typeStr,
                        targetDistanceKm = lDist,
                        targetPaceSec = targetPace,
                        description = desc,
                        instructions = instr,
                        isCompleted = false
                    )
                }
                WorkoutRole.EASY_1 -> createEasyRun(currentDate, weekNumber, (lDist * 0.5).coerceAtLeast(2.0), paceZones.easyPaceSec, level, isVeryBeginner, "Chạy Easy phục hồi")
                WorkoutRole.EASY_2 -> createEasyRun(currentDate, weekNumber, (lDist * 0.4).coerceAtLeast(2.0), paceZones.easyPaceSec, level, isVeryBeginner, "Chạy Easy duy trì")
                WorkoutRole.EASY_3 -> createEasyRun(currentDate, weekNumber, (lDist * 0.3).coerceAtLeast(2.0), paceZones.easyPaceSec, level, isVeryBeginner, "Chạy nhẹ nhàng")
                WorkoutRole.QUALITY -> {
                    val qDist = (lDist * 0.5).coerceAtLeast(2.0)
                    if (phase == Phase.BASE || isRecoveryWeek || level == FitnessLevel.BEGINNER) {
                        createEasyRun(currentDate, weekNumber, qDist, paceZones.easyPaceSec, level, isVeryBeginner, "Chạy Easy nền tảng")
                    } else {
                        if (phase == Phase.BUILD && weekNumber % 2 == 0 && level == FitnessLevel.ADVANCED) {
                            createIntervalRun(currentDate, weekNumber, qDist, paceZones.intervalPaceSec)
                        } else {
                            createTempoRun(currentDate, weekNumber, qDist, paceZones.tempoPaceSec)
                        }
                    }
                }
            }
            workouts.add(workout)
        }
        return workouts
    }

    private fun getRunWalkInstructions(level: FitnessLevel, weekNumber: Int, isVeryBeginner: Boolean): String {
        if (level != FitnessLevel.BEGINNER) {
            return "Chạy nhẹ nhàng thư giãn cơ thể. Giữ nhịp thở đều và ổn định."
        }
        return when {
            isVeryBeginner && weekNumber <= 3 -> 
                "Chiến thuật chạy/đi bộ: Chạy nhẹ 1 phút, đi bộ 1 phút. Lặp lại cho đến khi hoàn thành cự ly."
            weekNumber <= 4 -> 
                "Chiến thuật chạy/đi bộ: Chạy nhẹ 2 phút, đi bộ 1 phút. Lặp lại cho đến khi hoàn thành cự ly."
            weekNumber <= 8 -> 
                "Chiến thuật chạy/đi bộ: Chạy nhẹ 3 phút, đi bộ 1 phút. Lặp lại cho đến khi hoàn thành cự ly."
            else -> 
                "Chiến thuật chạy/đi bộ: Chạy nhẹ 4-5 phút, đi bộ 1 phút (hoặc chạy liên tục nếu thấy thoải mái)."
        }
    }

    private fun createEasyRun(date: LocalDate, weekNumber: Int, distance: Double, paceSec: Int, level: FitnessLevel, isVeryBeginner: Boolean, title: String): WorkoutEntity {
        val dist = Math.round(distance * 10.0) / 10.0
        val baseInstructions = getRunWalkInstructions(level, weekNumber, isVeryBeginner)
        val instructions = if (level == FitnessLevel.BEGINNER && weekNumber <= 3) {
            "Giai đoạn tạo thói quen chạy bộ. KHÔNG ĐẶT NẶNG QUÃNG ĐƯỜNG VÀ PACE. Hãy chạy thật chậm, đi bộ nghỉ ngơi thoải mái khi mệt. $baseInstructions"
        } else {
            baseInstructions
        }
        return WorkoutEntity(date = date.toString(), weekNumber = weekNumber, type = "EASY", targetDistanceKm = dist, targetPaceSec = paceSec, description = "$title (${dist}km)", instructions = instructions, isCompleted = false)
    }

    private fun createIntervalRun(date: LocalDate, weekNumber: Int, distance: Double, paceSec: Int): WorkoutEntity {
        val dist = Math.round(distance * 10.0) / 10.0
        return WorkoutEntity(date = date.toString(), weekNumber = weekNumber, type = "INTERVAL", targetDistanceKm = dist, targetPaceSec = paceSec, description = "Chạy Biến tốc (Interval) (${dist}km)", instructions = "Khởi động 2km. Chạy tốc độ rất nhanh (Interval Pace) trong 1km, đi bộ phục hồi 2 phút. Lặp lại.", isCompleted = false)
    }

    private fun createTempoRun(date: LocalDate, weekNumber: Int, distance: Double, paceSec: Int): WorkoutEntity {
        val dist = Math.round(distance * 10.0) / 10.0
        return WorkoutEntity(date = date.toString(), weekNumber = weekNumber, type = "TEMPO", targetDistanceKm = dist, targetPaceSec = paceSec, description = "Chạy Tempo nâng ngưỡng (${dist}km)", instructions = "Khởi động 1-2km. Chạy phần chính ở tốc độ 'mệt nhưng có thể duy trì' (Threshold Pace).", isCompleted = false)
    }

    private fun createRestWorkout(date: LocalDate, weekNumber: Int): WorkoutEntity {
        return WorkoutEntity(date = date.toString(), weekNumber = weekNumber, type = "REST", targetDistanceKm = 0.0, targetPaceSec = 0, description = "Nghỉ ngơi hoàn toàn", instructions = "Cơ thể phục hồi và phát triển trong những ngày nghỉ.", isCompleted = false)
    }
}
