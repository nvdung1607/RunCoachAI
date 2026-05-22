package com.example.runcoach.domain.plan

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.runcoach.data.local.db.WorkoutEntity
import java.io.OutputStream
import java.time.LocalDate

object PlanExporter {

    /**
     * Exports the workout plan to a CSV-formatted string.
     * Headers: Date, Week, Type, Target Distance (km), Target Pace (min/km), Description, Instructions, Completed, Actual Distance (km), Actual Duration (min), Notes
     */
    fun exportToCsv(workouts: List<WorkoutEntity>): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("Ngày,Tuần,Loại bài tập,Cự ly mục tiêu (km),Pace mục tiêu (phút/km),Mô tả,Hướng dẫn,Hoàn thành,Cự ly thực tế (km),Thời gian thực tế (phút),Ghi chú")

        val sorted = workouts.sortedBy { it.date }
        for (workout in sorted) {
            val paceStr = if (workout.targetPaceSec > 0) {
                val m = workout.targetPaceSec / 60
                val s = workout.targetPaceSec % 60
                "%d:%02d".format(m, s)
            } else ""

            val completed = if (workout.isCompleted) "Có" else "Không"

            // Escape CSV fields that might contain commas or quotes
            fun escape(s: String) = if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                "\"${s.replace("\"", "\"\"")}\""
            } else s

            sb.appendLine(
                listOf(
                    workout.date,
                    workout.weekNumber.toString(),
                    workout.type,
                    workout.targetDistanceKm.toString(),
                    paceStr,
                    escape(workout.description),
                    escape(workout.instructions),
                    completed,
                    workout.actualDistanceKm.toString(),
                    workout.actualDurationMin.toInt().toString(),
                    escape(workout.notes)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    /**
     * Returns a simple text representation suitable for sharing as plain text.
     */
    fun exportToText(workouts: List<WorkoutEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("=== KẾ HOẠCH LUYỆN TẬP RUNCOACH ===")
        sb.appendLine()

        val byWeek = workouts.groupBy { it.weekNumber }.toSortedMap()
        for ((week, weekWorkouts) in byWeek) {
            sb.appendLine("--- TUẦN $week ---")
            for (w in weekWorkouts.sortedBy { it.date }) {
                if (w.type == "REST") {
                    sb.appendLine("  ${w.date}: [Nghỉ ngơi]")
                } else {
                    val paceStr = if (w.targetPaceSec > 0) {
                        val m = w.targetPaceSec / 60
                        val s = w.targetPaceSec % 60
                        " - Pace: %d:%02d phút/km".format(m, s)
                    } else ""
                    val completedStr = if (w.isCompleted) " ✓" else ""
                    sb.appendLine("  ${w.date}: [${w.type}] ${w.targetDistanceKm} km$paceStr$completedStr")
                    sb.appendLine("    ${w.description}")
                }
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    /**
     * Exports the workout plan to a PDF document stream using Android's native PdfDocument.
     */
    fun exportToPdf(workouts: List<WorkoutEntity>, outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            strokeWidth = 1f
        }
        val textPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 18f
            color = android.graphics.Color.parseColor("#1E3A8A") // Dark blue color
            isFakeBoldText = true
            isAntiAlias = true
        }

        val sorted = workouts.sortedBy { it.date }
        val workoutsPerPage = 25
        val chunks = sorted.chunked(workoutsPerPage)

        for ((pageIndex, chunk) in chunks.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create() // A4 Size (595x842 points)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = 50f
            if (pageIndex == 0) {
                canvas.drawText("GIÁO ÁN LUYỆN TẬP RUNCOACH AI", 50f, y, titlePaint)
                y += 40f
            } else {
                canvas.drawText("KẾ HOẠCH LUYỆN TẬP (Tiếp theo) - Trang ${pageIndex + 1}", 50f, y, headerPaint)
                y += 30f
            }

            // Table Header Background
            val headerBgPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#F3F4F6")
            }
            canvas.drawRect(45f, y - 15f, 550f, y + 8f, headerBgPaint)

            // Draw table headers
            canvas.drawText("Ngày", 50f, y, headerPaint)
            canvas.drawText("Tuần", 130f, y, headerPaint)
            canvas.drawText("Loại", 175f, y, headerPaint)
            canvas.drawText("Cự ly", 240f, y, headerPaint)
            canvas.drawText("Pace mục tiêu", 300f, y, headerPaint)
            canvas.drawText("Mô tả", 380f, y, headerPaint)

            y += 8f
            canvas.drawLine(45f, y, 550f, y, paint)
            y += 18f

            for (workout in chunk) {
                canvas.drawText(workout.date, 50f, y, textPaint)
                canvas.drawText("Tuần ${workout.weekNumber}", 130f, y, textPaint)
                canvas.drawText(workout.type, 175f, y, textPaint)

                val distStr = if (workout.type == "REST") "-" else "${workout.targetDistanceKm} km"
                canvas.drawText(distStr, 240f, y, textPaint)

                val paceStr = if (workout.targetPaceSec > 0 && workout.type != "REST") {
                    val m = workout.targetPaceSec / 60
                    val s = workout.targetPaceSec % 60
                    "%d:%02d phút/km".format(m, s)
                } else "-"
                canvas.drawText(paceStr, 300f, y, textPaint)

                // Truncate description if too long
                val maxDescLen = 28
                val desc = if (workout.description.length > maxDescLen) {
                    workout.description.substring(0, maxDescLen - 3) + "..."
                } else {
                    workout.description
                }
                canvas.drawText(desc, 380f, y, textPaint)

                y += 24f
            }

            // Footer
            val footerPaint = Paint().apply {
                textSize = 9f
                color = android.graphics.Color.GRAY
                isAntiAlias = true
            }
            canvas.drawText("Được tạo bởi ứng dụng RunCoach AI - Trang ${pageIndex + 1}/${chunks.size}", 50f, 810f, footerPaint)

            pdfDocument.finishPage(page)
        }

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }
}
