package com.example.runcoach.domain.plan

import kotlin.math.exp
import kotlin.math.sqrt

data class PaceZones(
    val easyPaceSec: Int,
    val tempoPaceSec: Int,
    val longPaceSec: Int,
    val intervalPaceSec: Int
)

object VdotCalculator {

    /**
     * Calculates VDOT score based on time (in seconds) taken to run 3000m (3km).
     * Uses Jack Daniels' physiological equations.
     */
    fun calculateVdotFor3k(timeInSeconds: Double): Double {
        if (timeInSeconds <= 0.0) return 20.0
        val t = timeInSeconds / 60.0 // time in minutes
        val v = 3000.0 / t // speed in m/min

        // Oxygen cost of running at speed v
        val vo2Cost = -4.60 + 0.182258 * v + 0.000104 * v * v

        // Percent of VO2max sustained for duration t
        val d = 0.2989558 * exp(-0.1932605 * t) + 0.1894393 * exp(-0.012778 * t) + 0.8

        val vdot = vo2Cost / d
        // Cap VDOT to reasonable range (15 to 85)
        return vdot.coerceIn(15.0, 85.0)
    }

    /**
     * Solves Jack Daniels' quadratic equation to find the speed (m/min) at a given VO2 cost.
     * 0.000104 * v^2 + 0.182258 * v - (4.60 + vo2) = 0
     */
    private fun solveSpeedForVo2(vo2: Double): Double {
        val a = 0.000104
        val b = 0.182258
        val c = -4.60 - vo2

        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return 100.0 // safe fallback speed

        val v = (-b + sqrt(discriminant)) / (2 * a)
        return v
    }

    /**
     * Calculates pace zones in seconds per kilometer for a given VDOT score.
     */
    fun calculatePaceZones(vdot: Double): PaceZones {
        // Easy run intensity: ~65% VDOT
        val easyVo2 = vdot * 0.65
        val easySpeed = solveSpeedForVo2(easyVo2)
        val easyPace = (60000.0 / easySpeed).toInt().coerceIn(240, 720) // 4:00 to 12:00 /km

        // Long run intensity: ~62% VDOT (slightly slower/more relaxed than short easy runs)
        val longVo2 = vdot * 0.62
        val longSpeed = solveSpeedForVo2(longVo2)
        val longPace = (60000.0 / longSpeed).toInt().coerceIn(250, 750)

        // Tempo run intensity: ~85% VDOT
        val tempoVo2 = vdot * 0.85
        val tempoSpeed = solveSpeedForVo2(tempoVo2)
        val tempoPace = (60000.0 / tempoSpeed).toInt().coerceIn(180, 600) // 3:00 to 10:00 /km

        // Interval run intensity: ~98% VDOT
        val intervalVo2 = vdot * 0.98
        val intervalSpeed = solveSpeedForVo2(intervalVo2)
        val intervalPace = (60000.0 / intervalSpeed).toInt().coerceIn(150, 500)

        return PaceZones(
            easyPaceSec = easyPace,
            tempoPaceSec = tempoPace,
            longPaceSec = longPace,
            intervalPaceSec = intervalPace
        )
    }

    /**
     * Utility to format pace in seconds to String (MM:SS)
     */
    fun formatPace(paceSeconds: Int): String {
        val minutes = paceSeconds / 60
        val seconds = paceSeconds % 60
        return String.format("%d:%02d/km", minutes, seconds)
    }

    /**
     * Predicts race time in seconds for a given distance in kilometers and VDOT score.
     * Uses Jack Daniels' formula with binary search.
     */
    fun predictRaceTime(vdot: Double, distanceKm: Double): Double {
        if (vdot <= 0.0) return 0.0
        var low = 5.0 // minutes
        var high = 500.0 // minutes
        val distanceMeters = distanceKm * 1000.0
        
        for (i in 0..40) {
            val mid = (low + high) / 2.0
            val v = distanceMeters / mid // speed in m/min
            val vo2Cost = -4.60 + 0.182258 * v + 0.000104 * v * v
            val d = 0.2989558 * Math.exp(-0.1932605 * mid) + 0.1894393 * Math.exp(-0.012778 * mid) + 0.8
            val requiredVdot = vo2Cost / d
            if (requiredVdot > vdot) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0 * 60.0 // return in seconds
    }

    /**
     * Formats race time in seconds to HH:MM:SS format
     */
    fun formatDuration(totalSeconds: Double): String {
        val hrs = (totalSeconds / 3600).toInt()
        val mins = ((totalSeconds % 3600) / 60).toInt()
        val secs = (totalSeconds % 60).toInt()
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%d:%02d", mins, secs)
        }
    }
}
