package com.example.runcoach

import com.example.runcoach.domain.plan.VdotCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VdotCalculatorTest {

    @Test
    fun testVdotCalculationFor3k() {
        // 18 minutes (1080 seconds) for 3km
        val vdot = VdotCalculator.calculateVdotFor3k(1080.0)
        
        // Assert that VDOT is around 29.0
        assertTrue("VDOT should be around 29.0", vdot in 28.0..30.0)
    }

    @Test
    fun testPaceZonesCalculation() {
        val vdot = 30.0
        val zones = VdotCalculator.calculatePaceZones(vdot)
        
        // Easy run should be slower than long run, which is slower than tempo, which is slower than interval
        assertTrue("Easy pace should be slower than tempo pace", zones.easyPaceSec > zones.tempoPaceSec)
        assertTrue("Long pace should be slower than tempo pace", zones.longPaceSec > zones.tempoPaceSec)
        assertTrue("Tempo pace should be slower than interval pace", zones.tempoPaceSec > zones.intervalPaceSec)
        
        // Check bounds
        assertTrue("Easy pace should be in a reasonable range", zones.easyPaceSec in 240..720)
    }

    @Test
    fun testFormatPace() {
        val formatted = VdotCalculator.formatPace(485) // 8 minutes 5 seconds
        assertEquals("8:05\u00A0phút/km", formatted)
        
        val formatted2 = VdotCalculator.formatPace(360) // 6 minutes 0 seconds
        assertEquals("6:00\u00A0phút/km", formatted2)
    }

    @Test
    fun testGet3kTimeFromVdot() {
        val targetVdot = 35.0
        val timeSec = VdotCalculator.get3kTimeFromVdot(targetVdot)
        val calculatedVdot = VdotCalculator.calculateVdotFor3k(timeSec)
        
        assertEquals(targetVdot, calculatedVdot, 0.01)
    }
}
