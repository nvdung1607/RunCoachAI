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

object PlanGenerator {

    /**
     * Generates a complete daily training schedule from startDate until raceDate.
     * - Weeks always run Monday through Sunday.
     * - Long Run is always scheduled on Saturday.
     * - The plan is customized based on VDOT score, fitness level, and target distance.
     */
    fun generatePlan(
        startDate: LocalDate,
        raceDate: LocalDate,
        vdotScore: Double,
        level: FitnessLevel,
        targetDistance: Int = 21, // 5, 10, 21, 42
        maxSessionsPerWeek: Int = 3
    ): List<WorkoutEntity> {
        val workouts = mutableListOf<WorkoutEntity>()
        val totalDays = ChronoUnit.DAYS.between(startDate, raceDate)
        val totalWeeks = (totalDays / 7.0).coerceAtLeast(4.0).toInt() + 1

        val paceZones = VdotCalculator.calculatePaceZones(vdotScore)
        val isVeryBeginner = vdotScore < 25.0 && level == FitnessLevel.BEGINNER

        // Long Run is always on Saturday
        val longRunDay = DayOfWeek.SATURDAY

        val raceDist = when (targetDistance) {
            5 -> 5.0
            10 -> 10.0
            42 -> 42.2
            else -> 21.1
        }

        val racePace = when (targetDistance) {
            5 -> paceZones.intervalPaceSec
            10 -> paceZones.tempoPaceSec
            42 -> paceZones.longPaceSec
            else -> paceZones.tempoPaceSec // 21k
        }

        for (dayOffset in 0..totalDays) {
            val currentDate = startDate.plusDays(dayOffset)
            val startWeekMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeekMonday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekNumber = ChronoUnit.WEEKS.between(startWeekMonday, currentWeekMonday).toInt() + 1

            val currentDayOfWeek = currentDate.dayOfWeek

            val workout = when {
                // The actual race day
                currentDate.isEqual(raceDate) -> {
                    WorkoutEntity(
                        date = currentDate.toString(),
                        weekNumber = weekNumber,
                        type = "RACE",
                        targetDistanceKm = raceDist,
                        targetPaceSec = racePace,
                        description = "RACE DAY! Chinh phục cự ly $targetDistance km 🏁",
                        instructions = "Chúc mừng bạn! Hôm nay là ngày bạn tỏa sáng. Hãy khởi động kỹ, giữ tốc độ ổn định ở nửa đầu và bứt phá ở nửa cuối.",
                        isCompleted = false
                    )
                }

                // Race Week tapering (last week of plan)
                weekNumber == totalWeeks -> {
                    val easyDist = when (targetDistance) {
                        5 -> 2.0
                        10 -> 3.0
                        42 -> 6.0
                        else -> 3.0
                    }
                    val shakeoutDist = when (targetDistance) {
                        5 -> 1.5
                        10 -> 2.0
                        42 -> 4.0
                        else -> 2.0
                    }
                    when (currentDayOfWeek) {
                        DayOfWeek.TUESDAY -> WorkoutEntity(
                            date = currentDate.toString(),
                            weekNumber = weekNumber,
                            type = "EASY",
                            targetDistanceKm = easyDist,
                            targetPaceSec = paceZones.easyPaceSec,
                            description = "Chạy nhẹ thả lỏng trước race (${easyDist}km)",
                            instructions = "Chạy nhẹ nhàng thoải mái để giữ cảm giác chân. Đừng cố gắng quá sức.",
                            isCompleted = false
                        )
                        DayOfWeek.THURSDAY -> {
                            if (maxSessionsPerWeek <= 2) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                WorkoutEntity(
                                    date = currentDate.toString(),
                                    weekNumber = weekNumber,
                                    type = "EASY",
                                    targetDistanceKm = shakeoutDist,
                                    targetPaceSec = paceZones.easyPaceSec,
                                    description = "Chạy thả lỏng cuối cùng (${shakeoutDist}km)",
                                    instructions = "Chạy siêu nhẹ, thư giãn cơ thể. Nghỉ ngơi từ hôm nay đến race day.",
                                    isCompleted = false
                                )
                            }
                        }
                        else -> createRestOrCtWorkout(currentDate, weekNumber)
                    }
                }

                // Taper Week -1 (1 week before race)
                weekNumber == totalWeeks - 1 -> {
                    val taperLongDist = when (targetDistance) {
                        5 -> 5.0
                        10 -> 8.0
                        42 -> 20.0
                        else -> 12.0
                    }
                    val easyT1 = when (targetDistance) {
                        5 -> 3.0
                        10 -> 4.0
                        42 -> 8.0
                        else -> 5.0
                    }
                    val easyT2 = when (targetDistance) {
                        5 -> 2.0
                        10 -> 3.0
                        42 -> 6.0
                        else -> 4.0
                    }
                    when (currentDayOfWeek) {
                        longRunDay -> {
                            WorkoutEntity(
                                date = currentDate.toString(),
                                weekNumber = weekNumber,
                                type = "LONG",
                                targetDistanceKm = taperLongDist,
                                targetPaceSec = paceZones.longPaceSec,
                                description = "Chạy dài giảm tải (${taperLongDist}km)",
                                instructions = "Tuần giảm tải cận kề ngày race. Chạy nhẹ nhàng ở pace thoải mái.",
                                isCompleted = false
                            )
                        }
                        DayOfWeek.TUESDAY -> createEasyRun(currentDate, weekNumber, easyT1, paceZones.easyPaceSec, isVeryBeginner)
                        DayOfWeek.THURSDAY -> {
                            if (maxSessionsPerWeek <= 2) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                createEasyRun(currentDate, weekNumber, easyT2, paceZones.easyPaceSec, isVeryBeginner)
                            }
                        }
                        else -> createRestOrCtWorkout(currentDate, weekNumber)
                    }
                }

                // Taper Week -2 (2 weeks before race)
                weekNumber == totalWeeks - 2 -> {
                    val taperLongDist = when (targetDistance) {
                        5 -> 6.0
                        10 -> 10.0
                        42 -> 26.0
                        else -> 16.0
                    }
                    val easyT1 = when (targetDistance) {
                        5 -> 4.0
                        10 -> 5.0
                        42 -> 10.0
                        else -> 6.0
                    }
                    when (currentDayOfWeek) {
                        longRunDay -> WorkoutEntity(
                            date = currentDate.toString(),
                            weekNumber = weekNumber,
                            type = "LONG",
                            targetDistanceKm = taperLongDist,
                            targetPaceSec = paceZones.longPaceSec,
                            description = "Chạy dài taper (${taperLongDist}km)",
                            instructions = "Bài chạy dài chuẩn bị taper. Giữ pace chạy nhẹ, tiết kiệm năng lượng.",
                            isCompleted = false
                        )
                        DayOfWeek.TUESDAY -> createEasyRun(currentDate, weekNumber, easyT1, paceZones.easyPaceSec, isVeryBeginner)
                        DayOfWeek.THURSDAY -> {
                            if (maxSessionsPerWeek <= 2) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                createTempoOrEasy(currentDate, weekNumber, paceZones, level, totalWeeks, isVeryBeginner, targetDistance)
                            }
                        }
                        else -> createRestOrCtWorkout(currentDate, weekNumber)
                    }
                }

                // Peak Week (3 weeks before race)
                weekNumber == totalWeeks - 3 -> {
                    when (currentDayOfWeek) {
                        longRunDay -> {
                            val peakDistance = when (targetDistance) {
                                5 -> when (level) {
                                    FitnessLevel.BEGINNER -> 8.0
                                    FitnessLevel.INTERMEDIATE -> 9.0
                                    FitnessLevel.ADVANCED -> 10.0
                                }
                                10 -> when (level) {
                                    FitnessLevel.BEGINNER -> 12.0
                                    FitnessLevel.INTERMEDIATE -> 14.0
                                    FitnessLevel.ADVANCED -> 15.0
                                }
                                42 -> when (level) {
                                    FitnessLevel.BEGINNER -> 30.0
                                    FitnessLevel.INTERMEDIATE -> 32.0
                                    FitnessLevel.ADVANCED -> 35.0
                                }
                                else -> when (level) {
                                    FitnessLevel.BEGINNER -> 18.0
                                    FitnessLevel.INTERMEDIATE -> 20.0
                                    FitnessLevel.ADVANCED -> 21.0
                                }
                            }
                            WorkoutEntity(
                                date = currentDate.toString(),
                                weekNumber = weekNumber,
                                type = "LONG",
                                targetDistanceKm = peakDistance,
                                targetPaceSec = paceZones.longPaceSec,
                                description = "Chạy dài đỉnh điểm (${peakDistance}km) 🏆",
                                instructions = "Đây là buổi chạy dài quan trọng nhất trước race! Bổ sung nước đầy đủ và giữ pace thoải mái.",
                                isCompleted = false
                            )
                        }
                        DayOfWeek.TUESDAY -> createEasyRun(currentDate, weekNumber, calculateEasyDistance(weekNumber, totalWeeks, true, targetDistance), paceZones.easyPaceSec, isVeryBeginner)
                        DayOfWeek.THURSDAY -> {
                            if (maxSessionsPerWeek <= 2) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                createTempoOrEasy(currentDate, weekNumber, paceZones, level, totalWeeks, isVeryBeginner, targetDistance)
                            }
                        }
                        DayOfWeek.FRIDAY -> {
                            if (maxSessionsPerWeek <= 3) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                val recDist = when (targetDistance) {
                                    5 -> 2.0
                                    10 -> 3.0
                                    42 -> 6.0
                                    else -> 4.0
                                }
                                createRecoveryRun(currentDate, weekNumber, recDist, paceZones.easyPaceSec)
                            }
                        }
                        else -> createRestOrCtWorkout(currentDate, weekNumber)
                    }
                }

                // Regular training weeks
                else -> {
                    when (currentDayOfWeek) {
                        longRunDay -> {
                            val distance = calculateLongRunDistance(weekNumber, totalWeeks, level, targetDistance)
                            val instructions = if (isVeryBeginner && weekNumber <= 3) {
                                "Chiến thuật chạy/đi bộ: Chạy nhẹ 2 phút, đi bộ 1 phút. Lặp lại cho đến khi hoàn thành."
                            } else {
                                "Chạy ở tốc độ thoải mái, vừa chạy vừa nói chuyện được. Tập luyện uống nước sau mỗi 3-5km."
                            }
                            WorkoutEntity(
                                date = currentDate.toString(),
                                weekNumber = weekNumber,
                                type = "LONG",
                                targetDistanceKm = distance,
                                targetPaceSec = paceZones.longPaceSec,
                                description = "Chạy dài tích lũy sức bền (${distance}km)",
                                instructions = instructions,
                                isCompleted = false
                            )
                        }

                        DayOfWeek.SUNDAY -> createRestOrCtWorkout(currentDate, weekNumber)

                        DayOfWeek.TUESDAY -> {
                            val distance = calculateEasyDistance(weekNumber, totalWeeks, isTuesday = true, targetDistance = targetDistance)
                            createEasyRun(currentDate, weekNumber, distance, paceZones.easyPaceSec, isVeryBeginner)
                        }

                        DayOfWeek.THURSDAY -> {
                            if (maxSessionsPerWeek <= 2) {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            } else {
                                createTempoOrEasy(currentDate, weekNumber, paceZones, level, totalWeeks, isVeryBeginner, targetDistance)
                            }
                        }

                        DayOfWeek.WEDNESDAY -> {
                            val recDist = when (targetDistance) {
                                5 -> 2.0
                                10 -> 3.0
                                42 -> 6.0
                                else -> 3.0
                            }
                            if (weekNumber % 4 == 0 && maxSessionsPerWeek >= 4) {
                                createRecoveryRun(currentDate, weekNumber, recDist, paceZones.easyPaceSec)
                            } else {
                                createRestOrCtWorkout(currentDate, weekNumber)
                            }
                        }

                        else -> createRestWorkout(currentDate, weekNumber)
                    }
                }
            }
            workouts.add(workout)
        }
        return workouts
    }

    private fun createEasyRun(date: LocalDate, weekNumber: Int, distance: Double, paceSec: Int, isVeryBeginner: Boolean): WorkoutEntity {
        val instructions = if (isVeryBeginner && weekNumber <= 3) {
            "Chiến thuật chạy/đi bộ: Chạy nhẹ 1 phút, đi bộ 1 phút. Tổng cộng 20-30 phút."
        } else {
            "Chạy nhẹ nhàng thư giãn cơ thể. Giữ nhịp thở đều và ổn định. Tránh chạy quá nhanh."
        }
        return WorkoutEntity(
            date = date.toString(),
            weekNumber = weekNumber,
            type = "EASY",
            targetDistanceKm = distance,
            targetPaceSec = paceSec,
            description = "Chạy nhẹ thư giãn (${distance}km)",
            instructions = instructions,
            isCompleted = false
        )
    }

    private fun createRecoveryRun(date: LocalDate, weekNumber: Int, distance: Double, paceSec: Int): WorkoutEntity {
        return WorkoutEntity(
            date = date.toString(),
            weekNumber = weekNumber,
            type = "RECOVERY",
            targetDistanceKm = distance,
            targetPaceSec = paceSec + 30, // 30s/km slower than easy
            description = "Chạy phục hồi nhẹ (${distance}km)",
            instructions = "Chạy cực nhẹ nhàng để giúp cơ thể hồi phục nhanh hơn. Không cần ép pace.",
            isCompleted = false
        )
    }

    private fun createTempoOrEasy(
        date: LocalDate, weekNumber: Int, paceZones: PaceZones, level: FitnessLevel, totalWeeks: Int, isVeryBeginner: Boolean, targetDistance: Int
    ): WorkoutEntity {
        val isTempoWeek = level != FitnessLevel.BEGINNER && weekNumber % 3 == 0 && weekNumber < totalWeeks - 3
        return if (isTempoWeek) {
            val distance = when (targetDistance) {
                5 -> 3.0 + (weekNumber / 8.0)
                10 -> 4.0 + (weekNumber / 6.0)
                42 -> 8.0 + (weekNumber / 3.0)
                else -> 5.0 + (weekNumber / 4.0) // 21k
            }
            WorkoutEntity(
                date = date.toString(),
                weekNumber = weekNumber,
                type = "TEMPO",
                targetDistanceKm = distance,
                targetPaceSec = paceZones.tempoPaceSec,
                description = "Chạy Tempo nâng ngưỡng thể lực (${String.format("%.1f", distance)}km)",
                instructions = "Khởi động 1-2km chạy nhẹ. Chạy phần chính ở tốc độ 'mệt nhưng có thể duy trì'. Thả lỏng dãn cơ cuối bài.",
                isCompleted = false
            )
        } else {
            val distance = calculateEasyDistance(weekNumber, totalWeeks, isTuesday = false, targetDistance = targetDistance)
            createEasyRun(date, weekNumber, distance, paceZones.easyPaceSec, isVeryBeginner)
        }
    }

    private fun createRestOrCtWorkout(date: LocalDate, weekNumber: Int): WorkoutEntity {
        val dayOfWeek = date.dayOfWeek
        return if (dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            WorkoutEntity(
                date = date.toString(),
                weekNumber = weekNumber,
                type = "CT",
                targetDistanceKm = 0.0,
                targetPaceSec = 0,
                description = "Tập bổ trợ nhẹ nhàng (Bơi/Yoga/Đạp xe)",
                instructions = "Thực hiện các bài tập bổ trợ tim mạch ít chấn động như bơi lội, đạp xe hoặc yoga giãn cơ trong 30-45 phút.",
                isCompleted = false
            )
        } else {
            createRestWorkout(date, weekNumber)
        }
    }

    private fun createRestWorkout(date: LocalDate, weekNumber: Int): WorkoutEntity {
        return WorkoutEntity(
            date = date.toString(),
            weekNumber = weekNumber,
            type = "REST",
            targetDistanceKm = 0.0,
            targetPaceSec = 0,
            description = "Nghỉ ngơi hoàn toàn",
            instructions = "Cơ thể phục hồi và phát triển trong những ngày nghỉ. Hãy chú ý dinh dưỡng và ngủ đủ giấc.",
            isCompleted = false
        )
    }

    private fun calculateLongRunDistance(week: Int, totalWeeks: Int, level: FitnessLevel, targetDistance: Int): Double {
        val startDistance = when (targetDistance) {
            5 -> when (level) {
                FitnessLevel.BEGINNER -> 4.0
                FitnessLevel.INTERMEDIATE -> 5.0
                FitnessLevel.ADVANCED -> 6.0
            }
            10 -> when (level) {
                FitnessLevel.BEGINNER -> 6.0
                FitnessLevel.INTERMEDIATE -> 7.0
                FitnessLevel.ADVANCED -> 8.0
            }
            42 -> when (level) {
                FitnessLevel.BEGINNER -> 14.0
                FitnessLevel.INTERMEDIATE -> 16.0
                FitnessLevel.ADVANCED -> 18.0
            }
            else -> when (level) { // 21k
                FitnessLevel.BEGINNER -> 6.0
                FitnessLevel.INTERMEDIATE -> 8.0
                FitnessLevel.ADVANCED -> 10.0
            }
        }

        val peakDistance = when (targetDistance) {
            5 -> when (level) {
                FitnessLevel.BEGINNER -> 8.0
                FitnessLevel.INTERMEDIATE -> 9.0
                FitnessLevel.ADVANCED -> 10.0
            }
            10 -> when (level) {
                FitnessLevel.BEGINNER -> 12.0
                FitnessLevel.INTERMEDIATE -> 14.0
                FitnessLevel.ADVANCED -> 15.0
            }
            42 -> when (level) {
                FitnessLevel.BEGINNER -> 30.0
                FitnessLevel.INTERMEDIATE -> 32.0
                FitnessLevel.ADVANCED -> 35.0
            }
            else -> when (level) { // 21k
                FitnessLevel.BEGINNER -> 18.0
                FitnessLevel.INTERMEDIATE -> 20.0
                FitnessLevel.ADVANCED -> 21.0
            }
        }

        val cycleIndex = (week - 1) % 4
        val adjustedWeek = week - (week / 4)

        val step = when (targetDistance) {
            5 -> 1.0
            10 -> 1.5
            42 -> 3.0
            else -> 2.0
        }

        return if (cycleIndex == 3) {
            val prevDistance = startDistance + (adjustedWeek - 1) * step
            (prevDistance * 0.75).coerceAtLeast(startDistance).coerceAtMost(peakDistance)
        } else {
            (startDistance + adjustedWeek * step).coerceAtMost(peakDistance)
        }
    }

    private fun calculateEasyDistance(week: Int, totalWeeks: Int, isTuesday: Boolean, targetDistance: Int): Double {
        val weeksBeforeRace = totalWeeks - week
        if (weeksBeforeRace <= 1) {
            return when (targetDistance) {
                5 -> 2.0
                10 -> 3.0
                42 -> 5.0
                else -> 3.0
            }
        }
        if (weeksBeforeRace == 2) {
            return when (targetDistance) {
                5 -> 3.0
                10 -> 4.0
                42 -> 6.0
                else -> 4.0
            }
        }

        val baseDistance = when (targetDistance) {
            5 -> if (isTuesday) 2.5 else 3.0
            10 -> if (isTuesday) 3.5 else 4.0
            42 -> if (isTuesday) 6.0 else 8.0
            else -> if (isTuesday) 4.0 else 5.0 // 21k
        }

        val maxDist = when (targetDistance) {
            5 -> 5.0
            10 -> 7.0
            42 -> 14.0
            else -> 9.0
        }

        val progression = (week / 4).toDouble() * when (targetDistance) {
            5 -> 0.5
            10 -> 0.8
            42 -> 1.5
            else -> 1.0
        }

        return (baseDistance + progression).coerceAtMost(maxDist)
    }
}
