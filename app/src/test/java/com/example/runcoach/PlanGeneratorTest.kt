package com.example.runcoach

import com.example.runcoach.domain.plan.FitnessLevel
import com.example.runcoach.domain.plan.PlanGenerator
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
}
