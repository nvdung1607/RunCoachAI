package com.example.runcoach.domain.plan

data class FeasibilityReport(
    val isFeasible: Boolean,
    val warningMessage: String?,
    val recommendation: String?
)

object PlanFeasibilityChecker {
    /**
     * Checks the feasibility of a training plan based on:
     * - targetDistance (5k, 10k, 21k, 42k)
     * - level (FitnessLevel)
     * - weeks (preparation weeks)
     * - time3kSeconds (result from 3km test run)
     */
    fun checkFeasibility(
        targetDistance: Int,
        level: FitnessLevel,
        weeks: Int,
        time3kSeconds: Double
    ): FeasibilityReport {
        val vdot = VdotCalculator.calculateVdotFor3k(time3kSeconds)
        val paceMinKm = (time3kSeconds / 3.0) / 60.0 // minutes per km

        // 1. Marathon (42km)
        if (targetDistance == 42) {
            // Check for extreme case: very short duration or low fitness
            if (level == FitnessLevel.BEGINNER || paceMinKm >= 8.0 || vdot < 25.0) {
                if (weeks < 16) {
                    val warning = "Chu kỳ chuẩn bị hiện tại là $weeks tuần với thể trạng hiện tại (chạy 3km hết ${String.format("%.1f", time3kSeconds / 60.0)} phút, pace ${String.format("%.2f", paceMinKm)}/km) là cực kỳ khó khả thi và có nguy cơ cao gây chấn thương nghiêm trọng."
                    val recommendation = "Đối với cự ly Marathon (42km) cho người mới:\n" +
                            "• Thời gian chuẩn bị khuyên dùng tối thiểu là 16-24 tuần (khoảng 4-6 tháng) để cơ thể tích lũy hệ cơ xương khớp.\n" +
                            "• Bạn nên bắt đầu bằng cách chọn mục tiêu cự ly ngắn hơn như 5km hoặc 10km để làm quen, hoặc kéo dài thời gian chuẩn bị bằng cách chọn ngày đua xa hơn."
                    return FeasibilityReport(false, warning, recommendation)
                }
            }
            if (weeks < 8) {
                val warning = "Chuẩn bị cho cự ly Marathon 42km chỉ trong $weeks tuần là bất khả thi đối với hầu hết mọi người và vô cùng nguy hiểm."
                val recommendation = "Giải pháp an toàn:\n" +
                        "• Thay đổi mục tiêu giải chạy sang cự ly nhỏ hơn (5k/10k).\n" +
                        "• Thay đổi ngày giải chạy để kéo dài thời gian chuẩn bị lên ít nhất 12-16 tuần."
                return FeasibilityReport(false, warning, recommendation)
            }
        }

        // 2. Half Marathon (21km)
        if (targetDistance == 21) {
            if (level == FitnessLevel.BEGINNER || paceMinKm >= 9.0 || vdot < 20.0) {
                if (weeks < 12) {
                    val warning = "Chu kỳ chuẩn bị $weeks tuần để chạy cự ly Bán marathon (21km) là quá ngắn so với thể trạng hiện tại của bạn."
                    val recommendation = "Khuyến nghị:\n" +
                            "• Tăng thời gian tích lũy bằng cách chọn ngày chạy mục tiêu ra thêm ít nhất 8 tuần (tổng cộng khoảng 12-16 tuần).\n" +
                            "• Hoặc hạ mục tiêu xuống cự ly 5km hoặc 10km trước khi chinh phục 21km."
                    return FeasibilityReport(false, warning, recommendation)
                }
            }
            if (weeks < 6) {
                val warning = "Chuẩn bị chạy 21km trong vòng $weeks tuần là rất nguy hiểm cho hệ tim mạch và cơ xương khớp của bạn."
                val recommendation = "Khuyến nghị: Thay đổi ngày giải chạy ra xa (tối thiểu 8-12 tuần chuẩn bị) hoặc chuyển sang mục tiêu 5km."
                return FeasibilityReport(false, warning, recommendation)
            }
        }

        // 3. 10km
        if (targetDistance == 10) {
            if (level == FitnessLevel.BEGINNER && weeks < 6) {
                val warning = "Thời gian chuẩn bị $weeks tuần cho cự ly 10km đối với người mới bắt đầu là khá gấp gáp."
                val recommendation = "Khuyến nghị: Kéo dài thời gian chuẩn bị lên ít nhất 8-10 tuần, hoặc tập trung hoàn thành cự ly 5km trước."
                return FeasibilityReport(false, warning, recommendation)
            }
        }

        return FeasibilityReport(true, null, null)
    }
}
