package com.example.runcoach

import com.example.runcoach.domain.plan.FitnessLevel
import com.example.runcoach.domain.plan.PlanGenerator
import com.example.runcoach.domain.plan.PlanFeasibilityChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanGeneratorTest {

    @Test
    fun testPlanGenerationBasicProperties() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22) // ~13 weeks
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 30.0,
            level = FitnessLevel.BEGINNER
        )

        assertNotNull(plan)
        assertTrue("Plan should have workouts", plan.isNotEmpty())

        // Check if there is a race day workout at the end
        val raceWorkout = plan.last()
        assertEquals("RACE", raceWorkout.type)
        assertEquals("RACE DAY! Chinh phục cự ly 21.0975 km", raceWorkout.description)
        assertEquals("2026-08-22", raceWorkout.date)

        // Check that workouts have week numbers
        val weekNumbers = plan.map { it.weekNumber }.distinct()
        assertTrue("Should have multiple weeks", weekNumbers.size >= 13)
        assertEquals(1, weekNumbers.minOrNull())

        // Beginner plan should have lots of REST days
        val restDaysCount = plan.count { it.type == "REST" }
        assertTrue("Beginner plan should contain rest days", restDaysCount > 0)
    }

    @Test
    fun testPlanTapering() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22)
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 32.0,
            level = FitnessLevel.BEGINNER
        )

        // Long runs in weeks 10-11 should be longer than week 13 (tapering week)
        val week10LongRun = plan.firstOrNull { it.weekNumber == 10 && it.type == "LONG" }
        val week13LongRun = plan.firstOrNull { it.weekNumber == 13 && it.type == "LONG" }

        if (week10LongRun != null && week13LongRun != null) {
            assertTrue(
                "Week 10 long run should be longer or equal to Week 13 tapering long run",
                week10LongRun.targetDistanceKm >= week13LongRun.targetDistanceKm
            )
        }
    }

    @Test
    fun testMaxSessionsPerWeek() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22)
        
        val plan3 = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 32.0,
            level = FitnessLevel.INTERMEDIATE,
            targetDistance = 21,
            maxSessionsPerWeek = 3
        )

        val runsByWeek3 = plan3.groupBy { it.weekNumber }
        for ((week, workouts) in runsByWeek3) {
            val runsCount = workouts.count { it.type !in listOf("REST", "CT") }
            assertTrue("Week $week should have at most 3 runs, got $runsCount", runsCount <= 3)
        }

        val plan2 = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 32.0,
            level = FitnessLevel.INTERMEDIATE,
            targetDistance = 21,
            maxSessionsPerWeek = 2
        )

        val runsByWeek2 = plan2.groupBy { it.weekNumber }
        for ((week, workouts) in runsByWeek2) {
            val runsCount = workouts.count { it.type !in listOf("REST", "CT") }
            assertTrue("Week $week should have at most 2 runs, got $runsCount", runsCount <= 2)
        }
    }

    @Test
    fun testPlanProgressiveLoadingAndRecovery() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22) // ~13 weeks, totalWeeks = 14, peakWeek = 11
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 40.0,
            level = FitnessLevel.INTERMEDIATE,
            targetDistance = 21
        )

        val longRuns = plan.filter { it.type.startsWith("LONG") }.associateBy { it.weekNumber }
        
        // Week 4 should be a recovery week (w % 4 == 0 && w < 11)
        val week3Dist = longRuns[3]?.targetDistanceKm ?: 0.0
        val week4Dist = longRuns[4]?.targetDistanceKm ?: 0.0
        assertTrue("Week 4 should be a recovery week and be less than Week 3", week4Dist < week3Dist)
        // Week 4 base is 11.75, 75% of 11.75 is 8.81 -> 8.8
        assertEquals(8.8, week4Dist, 0.05)

        // Week 8 should also be a recovery week
        val week7Dist = longRuns[7]?.targetDistanceKm ?: 0.0
        val week8Dist = longRuns[8]?.targetDistanceKm ?: 0.0
        assertTrue("Week 8 should be a recovery week and be less than Week 7", week8Dist < week7Dist)
        // Week 8 base is 16.0, 75% of 16.0 is 12.0
        assertEquals(12.0, week8Dist, 0.05)

        // Week 11 is Peak Week, should have peak distance (18.0 km)
        val week11Dist = longRuns[11]?.targetDistanceKm ?: 0.0
        assertEquals(18.0, week11Dist, 0.01)

        // Week 12 (Taper 2) should be 80% of peak (14.4 km)
        val week12Dist = longRuns[12]?.targetDistanceKm ?: 0.0
        assertEquals(14.4, week12Dist, 0.01)

        // Week 13 (Taper 1) should be 60% of peak (10.8 km)
        val week13Dist = longRuns[13]?.targetDistanceKm ?: 0.0
        assertEquals(10.8, week13Dist, 0.01)
    }

    @Test
    fun testPreferredLongRunDay() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22)
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 40.0,
            level = FitnessLevel.INTERMEDIATE,
            targetDistance = 21,
            maxSessionsPerWeek = 4,
            preferredLongRunDay = java.time.DayOfWeek.SATURDAY
        )

        val longRuns = plan.filter { it.type.startsWith("LONG") }
        for (run in longRuns) {
            val date = LocalDate.parse(run.date)
            assertEquals(java.time.DayOfWeek.SATURDAY, date.dayOfWeek)
        }
    }

    @Test
    fun testPlanFeasibilityChecker() {
        val reportMarathonUnfeasible = PlanFeasibilityChecker.checkFeasibility(
            targetDistance = 42,
            level = FitnessLevel.BEGINNER,
            weeks = 8,
            time3kSeconds = 1200.0
        )
        assertNotNull(reportMarathonUnfeasible)
        assertEquals(false, reportMarathonUnfeasible.isFeasible)
        assertTrue(reportMarathonUnfeasible.warningMessage?.contains("bất khả thi") == true || reportMarathonUnfeasible.warningMessage?.contains("cực kỳ khó khả thi") == true)

        val reportMarathonFeasible = PlanFeasibilityChecker.checkFeasibility(
            targetDistance = 42,
            level = FitnessLevel.ADVANCED,
            weeks = 16,
            time3kSeconds = 720.0
        )
        assertTrue(reportMarathonFeasible.isFeasible)

        val reportHMUnfeasible = PlanFeasibilityChecker.checkFeasibility(
            targetDistance = 21,
            level = FitnessLevel.BEGINNER,
            weeks = 4,
            time3kSeconds = 1500.0
        )
        assertEquals(false, reportHMUnfeasible.isFeasible)

        val report10kFeasible = PlanFeasibilityChecker.checkFeasibility(
            targetDistance = 10,
            level = FitnessLevel.BEGINNER,
            weeks = 8,
            time3kSeconds = 1200.0
        )
        assertTrue(report10kFeasible.isFeasible)
    }

    @Test
    fun testBeginnerHabitBuildingWeeks() {
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22)
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 24.0,
            level = FitnessLevel.BEGINNER,
            targetDistance = 21
        )

        val longRuns = plan.filter { it.type.startsWith("LONG") }.associateBy { it.weekNumber }

        // Week 1's long run is overridden to EASY because it is within the first 3 active workouts.
        // So longRuns[1] is null.
        // But Week 2 and Week 3 long runs are preserved as LONG runs.
        assertEquals(6.0, longRuns[2]?.targetDistanceKm ?: 0.0, 0.01)
        assertEquals(7.0, longRuns[3]?.targetDistanceKm ?: 0.0, 0.01)

        val week1Workouts = plan.filter { it.weekNumber == 1 && it.type != "REST" }
        val week2Workouts = plan.filter { it.weekNumber == 2 && it.type != "REST" }
        val week3Workouts = plan.filter { it.weekNumber == 3 && it.type != "REST" }

        assertTrue(week1Workouts.any { it.instructions.contains("Chạy nhẹ 1 phút, đi bộ 1 phút") })
        assertTrue(week2Workouts.any { it.instructions.contains("Chạy nhẹ 1 phút, đi bộ 1 phút") })
        assertTrue(week3Workouts.any { it.instructions.contains("Chạy nhẹ 1 phút, đi bộ 1 phút") })
    }

    @Test
    fun testFirstWorkoutIsTodayAndEasy() {
        // Today is Wednesday
        val startDate = LocalDate.of(2026, 5, 20)
        val raceDate = LocalDate.of(2026, 8, 22)
        
        val plan = PlanGenerator.generatePlan(
            startDate = startDate,
            raceDate = raceDate,
            vdotScore = 30.0,
            level = FitnessLevel.BEGINNER,
            targetDistance = 21,
            maxSessionsPerWeek = 3
        )

        // 1. The first day of the plan (which is today, 2026-05-20) must be an EASY run
        val todayWorkout = plan.firstOrNull { it.date == "2026-05-20" }
        assertNotNull("First day should have a workout", todayWorkout)
        assertEquals("EASY", todayWorkout?.type)

        // 2. The first 3 active workouts of the plan must be EASY runs
        val activeWorkouts = plan.filter { it.type != "REST" && it.type != "RACE" }
        assertTrue("Should have at least 3 active workouts", activeWorkouts.size >= 3)
        
        assertEquals("First active workout should be EASY", "EASY", activeWorkouts[0].type)
        assertEquals("Second active workout should be EASY", "EASY", activeWorkouts[1].type)
        assertEquals("Third active workout should be EASY", "EASY", activeWorkouts[2].type)
    }
}
