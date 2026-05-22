package com.example.runcoach.presentation.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.runcoach.data.health.HealthConnectManager
import com.example.runcoach.data.local.db.WorkoutEntity
import com.example.runcoach.data.local.preferences.UserPreferences
import com.example.runcoach.domain.plan.VdotCalculator
import com.example.runcoach.domain.plan.FitnessLevel
import com.example.runcoach.domain.plan.PlanFeasibilityChecker
import com.example.runcoach.domain.plan.FeasibilityReport
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.toArgb

// ─────────────────────────────────────────────
// ONBOARDING SCREEN
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onNavigateToTestRun: () -> Unit
) {
    var raceDateText by remember { mutableStateOf(LocalDate.now().plusMonths(3).toString()) }
    var fitnessLevel by remember { mutableStateOf("BEGINNER") }
    var targetDistance by remember { mutableIntStateOf(21) }
    var maxSessions by remember { mutableIntStateOf(3) }
    var gender by remember { mutableStateOf("MALE") }
    var age by remember { mutableIntStateOf(25) }
    var activityLevel by remember { mutableStateOf("SEDENTARY") }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF0F0F10), Color(0xFF1E1B4B))
        else listOf(Color(0xFFFAFAFA), Color(0xFFEEF2FF))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            // Logo
            val logoResId = if (isDark) com.example.runcoach.R.drawable.app_logo_dark else com.example.runcoach.R.drawable.app_logo_light
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "RunCoach Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("RunCoach AI", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Cá nhân hóa lộ trình tập luyện của riêng bạn",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Thiết lập mục tiêu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val parsedDate = try {
                                    LocalDate.parse(raceDateText)
                                } catch (e: Exception) {
                                    LocalDate.now().plusMonths(3)
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                        raceDateText = selectedDate.toString()
                                    },
                                    parsedDate.year,
                                    parsedDate.monthValue - 1,
                                    parsedDate.dayOfMonth
                                ).apply {
                                    datePicker.minDate = System.currentTimeMillis()
                                    show()
                                }
                            }
                    ) {
                        OutlinedTextField(
                            value = raceDateText,
                            onValueChange = {},
                            label = { Text("Ngày diễn ra giải chạy (Race Day)") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "Nhấp chọn để mở lịch. Hệ thống khuyên dùng tối thiểu 4-12 tuần.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Cự ly mục tiêu", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    val distances = listOf(5, 10, 21, 42)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        distances.forEach { dist ->
                            val isSelected = targetDistance == dist
                            val distText = when(dist) {
                                5 -> "5K"
                                10 -> "10K"
                                21 -> "21K"
                                42 -> "42K"
                                else -> "${dist}K"
                            }
                            val labelText = when(dist) {
                                5 -> "5 km"
                                10 -> "10 km"
                                21 -> "21.1 km"
                                42 -> "42.2 km"
                                else -> "$dist km"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { targetDistance = dist }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = distText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = labelText,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Số buổi tập tối đa / tuần", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    val sessionsOptions = listOf(2, 3, 4, 5)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {
                        sessionsOptions.forEach { sess ->
                            val isSelected = maxSessions == sess
                            val label = when(sess) {
                                3 -> "3 buổi\n(Gợi ý)"
                                else -> "$sess buổi\n "
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { maxSessions = sess }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Thể trạng hiện tại của bạn", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    val levels = listOf(
                        "BEGINNER" to "🟢 Người mới (Ít vận động)",
                        "INTERMEDIATE" to "🔵 Trung bình (Có thể chạy liên tục 5km)",
                        "ADVANCED" to "🔴 Nâng cao (Chạy thường xuyên trên 10km)"
                    )

                    levels.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (fitnessLevel == key) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable { fitnessLevel = key }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(selected = (fitnessLevel == key), onClick = { fitnessLevel = key })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Giới tính", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("♂️ Nam" to "MALE", "♀️ Nữ" to "FEMALE", "⚧️ Khác" to "OTHER").forEach { (label, value) ->
                            val isSelected = gender == value
                            val selectedBgColor = when (value) {
                                "MALE" -> Color(0xFF3B82F6)
                                "FEMALE" -> Color(0xFFEC4899)
                                "OTHER" -> Color(0xFF8B5CF6)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) selectedBgColor
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) selectedBgColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { gender = value }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Tuổi của bạn: $age tuổi", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = age.toFloat(),
                        onValueChange = { age = it.toInt() },
                        valueRange = 14f..75f,
                        steps = 60,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("14 tuổi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("75 tuổi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    if (age >= 40) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ℹ️ Giáo án sẽ tự điều chỉnh với thêm ngày nghỉ hồi phục phù hợp cho runner ${if (age >= 50) "50+" else "40+"}.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    try {
                        val parsedDate = LocalDate.parse(raceDateText)
                        if (parsedDate.isBefore(LocalDate.now())) {
                            Toast.makeText(context, "Ngày chạy phải ở tương lai!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val diffWeeks = ChronoUnit.WEEKS.between(LocalDate.now(), parsedDate)
                        if (diffWeeks < 4) {
                            Toast.makeText(context, "Thời gian chuẩn bị tối thiểu phải từ 4 tuần!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveOnboarding(raceDateText, fitnessLevel, targetDistance, maxSessions, gender, age, activityLevel)
                        onNavigateToTestRun()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Định dạng ngày chưa đúng YYYY-MM-DD!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Tiếp Tục →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Phiên bản 1.3",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// TEST RUN SCREEN
// ─────────────────────────────────────────────
@Composable
fun TestRunScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    var minText by remember { mutableStateOf("") }
    var secText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    var showFeasibilityDialog by remember { mutableStateOf(false) }
    var feasibilityReport by remember { mutableStateOf<FeasibilityReport?>(null) }
    var pendingTotalSec by remember { mutableStateOf(0.0) }
    var showPaceGuide by remember { mutableStateOf(false) }
    var showHowToMeasure by remember { mutableStateOf(false) }

    // Pulsating animation for the "Ôi chưa biết" button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthConnectManager.requiredPermissions)) {
            Toast.makeText(context, "Kết nối Health Connect thành công!", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                val today = Instant.now()
                val thirtyDaysAgo = today.minus(java.time.Duration.ofDays(30))
                val sessions = healthConnectManager.getRunningSessions(thirtyDaysAgo, today)
                val best3k = sessions.filter { it.distanceKm >= 2.9 }.minByOrNull { it.durationMinutes / it.distanceKm }
                if (best3k != null) {
                    val paceMinPerKm = best3k.durationMinutes / best3k.distanceKm
                    val total3kSeconds = paceMinPerKm * 3.0 * 60.0
                    minText = (total3kSeconds / 60).toInt().toString()
                    secText = (total3kSeconds % 60).toInt().toString()
                    Toast.makeText(context, "Đã tự lấy kết quả chạy tốt nhất từ Health Connect!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không tìm thấy bài chạy 3km nào gần đây.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Bạn đã từ chối cấp quyền Health Connect.", Toast.LENGTH_SHORT).show()
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF0F0F10), Color(0xFF1E293B))
        else listOf(Color(0xFFFAFAFA), Color(0xFFF1F5F9))
    )

    // Check Health Connect availability
    val hcStatus = remember {
        try { HealthConnectClient.getSdkStatus(context) } catch (e: Exception) { -1 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Bài Kiểm Tra Thể Lực", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Chạy 3km hết sức để đo lường khả năng tim mạch (VDOT). Kết quả này sẽ cá nhân hóa toàn bộ giáo án của bạn.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Thời gian chạy 3km tốt nhất", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Phút") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secText,
                            onValueChange = { secText = it },
                            label = { Text("Giây") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // VDOT Preview - realtime feedback
                    val previewTotalSec = remember(minText, secText) {
                        val m = minText.toIntOrNull() ?: 0
                        val s = secText.toIntOrNull() ?: 0
                        val total = m * 60.0 + s
                        if (total in 360.0..2400.0) total else -1.0
                    }
                    AnimatedVisibility(visible = previewTotalSec > 0) {
                        val vdotPreview = remember(previewTotalSec) {
                            if (previewTotalSec > 0) VdotCalculator.calculateVdotFor3k(previewTotalSec) else 0.0
                        }
                        val pacesPreview = remember(vdotPreview) {
                            if (vdotPreview > 0) VdotCalculator.calculatePaceZones(vdotPreview) else null
                        }
                        if (pacesPreview != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "⚡ Thể lực VDOT ước lượng: ${"%.1f".format(vdotPreview)}",
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Easy: ${VdotCalculator.formatPace(pacesPreview.easyPaceSec)}   " +
                                            "Tempo: ${VdotCalculator.formatPace(pacesPreview.tempoPaceSec)}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showPaceGuide = !showPaceGuide },
                            modifier = Modifier
                                .weight(1f)
                                .scale(pulseScale)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ôi chưa biết", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showHowToMeasure = !showHowToMeasure },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cách đo thời gian", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Pace reference guide (expandable)
                    AnimatedVisibility(visible = showPaceGuide) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "📊 Bảng Tham Khảo Thời Gian Chạy 3km",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val entries = listOf(
                                    Triple("🐣 Mới bắt đầu hoàn toàn", "8-10 ph/km", "24-30 phút"),
                                    Triple("🎣 Đã tập đôi chút", "6-8 ph/km", "18-24 phút"),
                                    Triple("🚴 Chạy đều đặn", "5-6 ph/km", "15-18 phút"),
                                    Triple("🔥 Chạy khá tốt", "4:30-5 ph/km", "13-15 phút"),
                                    Triple("🏆 Runner chuyên", "< 4:30 ph/km", "< 13:30 phút")
                                )
                                entries.forEach { (group, _, time) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(group, fontSize = 14.sp, modifier = Modifier.weight(1.4f),
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f),
                                            textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // How to measure guide (expandable)
                    AnimatedVisibility(visible = showHowToMeasure) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "🏃 Cách Đo Thời Gian Chạy 3km",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val steps = listOf(
                                    "📍 Tìm một sân vận động có đường chạy chuẩn (400m/vòng) hoặc một con đường bằng phẳng.",
                                    "📱 Mở đồng hồ bấm giờ trên điện thoại hoặc đồng hồ thể thao.",
                                    "🏃 Chạy 3km (7.5 vòng sân chuẩn) với nỗ lực tối đa của bạn.",
                                    "⏱ Dừng bấm giờ ngay khi về đích 3km và ghi lại kết quả.",
                                    "🎥 Hoặc dùng Strava, Garmin chạy một đoạn bất kỳ và tập ứng dụng sẽ tự đồng bộ qua Health Connect!"
                                )
                                steps.forEachIndexed { i, step ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("${i + 1}.", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 6.dp, top = 1.dp))
                                        Text(step, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Health Connect status & button
                    when (hcStatus) {
                        HealthConnectClient.SDK_AVAILABLE -> {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (healthConnectManager.hasPermissions()) {
                                            val today = Instant.now()
                                            val thirtyDaysAgo = today.minus(java.time.Duration.ofDays(30))
                                            val sessions = healthConnectManager.getRunningSessions(thirtyDaysAgo, today)
                                            val best3k = sessions.filter { it.distanceKm >= 2.9 }.minByOrNull { it.durationMinutes / it.distanceKm }
                                            if (best3k != null) {
                                                val total3kSeconds = (best3k.durationMinutes / best3k.distanceKm) * 3.0 * 60.0
                                                minText = (total3kSeconds / 60).toInt().toString()
                                                secText = (total3kSeconds % 60).toInt().toString()
                                                Toast.makeText(context, "Đã đồng bộ từ Health Connect!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Không tìm thấy bài chạy 3km gần đây.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            permissionsLauncher.launch(healthConnectManager.requiredPermissions)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF4285F4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đồng bộ từ Health Connect", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        HealthConnectClient.SDK_UNAVAILABLE -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ColorWarning.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ColorWarning.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ColorWarning, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Health Connect không khả dụng trên thiết bị này. Vui lòng nhập thời gian thủ công.",
                                        fontSize = 14.sp,
                                        color = ColorWarning
                                    )
                                }
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                        }
                                        context.startActivity(installIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Không thể mở CH Play.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cài đặt Health Connect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val min = minText.toIntOrNull() ?: 0
                    val sec = secText.toIntOrNull() ?: 0
                    val totalSec = min * 60.0 + sec
                    if (totalSec < 360.0 || totalSec > 2400.0) {
                        Toast.makeText(context, "Nhập thời gian hợp lệ cho 3km (6–40 phút)!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val raceDateParsed = try {
                        LocalDate.parse(userPrefs.raceDate)
                    } catch (e: Exception) {
                        LocalDate.now().plusWeeks(8)
                    }
                    val totalDays = ChronoUnit.DAYS.between(LocalDate.now(), raceDateParsed)
                    val totalWeeks = (totalDays / 7.0).coerceAtLeast(4.0).toInt() + 1

                    val level = when (userPrefs.fitnessLevel) {
                        "INTERMEDIATE" -> FitnessLevel.INTERMEDIATE
                        "ADVANCED" -> FitnessLevel.ADVANCED
                        else -> FitnessLevel.BEGINNER
                    }

                    val report = PlanFeasibilityChecker.checkFeasibility(
                        targetDistance = userPrefs.targetDistance,
                        level = level,
                        weeks = totalWeeks,
                        time3kSeconds = totalSec
                    )

                    if (report.isFeasible) {
                        viewModel.completeTestRun(totalSec) {
                            onNavigateToDashboard()
                        }
                    } else {
                        pendingTotalSec = totalSec
                        feasibilityReport = report
                        showFeasibilityDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Sinh Giáo Án & Bắt Đầu →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showFeasibilityDialog && feasibilityReport != null) {
            AlertDialog(
                onDismissRequest = { showFeasibilityDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ColorWarning, modifier = Modifier.size(36.dp)) },
                title = {
                    Text(
                        text = "Cảnh Báo Tính Khả Thi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        feasibilityReport?.warningMessage?.let { warning ->
                            Text(
                                text = warning,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            )
                        }
                        feasibilityReport?.recommendation?.let { recommendation ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Text(
                                text = recommendation,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFeasibilityDialog = false
                            viewModel.completeTestRun(pendingTotalSec) {
                                onNavigateToDashboard()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Tôi vẫn muốn tiếp tục")
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                showFeasibilityDialog = false
                                onNavigateToOnboarding()
                            }
                        ) {
                            Text("Điều chỉnh mục tiêu")
                        }
                        TextButton(
                            onClick = { showFeasibilityDialog = false }
                        ) {
                            Text("Hủy", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// DASHBOARD SCREEN
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPlan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    val workoutsList by viewModel.workouts.collectAsState()
    val todayWorkout by viewModel.todayWorkout.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }
    var logDistanceText by remember { mutableStateOf("") }
    var logDurationText by remember { mutableStateOf("") }
    var selectedRpe by remember { mutableStateOf(3) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showNotifSettings by remember { mutableStateOf(false) }
    var showWorkoutDetails by remember { mutableStateOf<WorkoutEntity?>(null) }
    var syncResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingSyncProposal by remember { mutableStateOf<com.example.runcoach.presentation.ProposedSync?>(null) }
    
    // Dialog states for informational popups
    var showRaceDayInfo by remember { mutableStateOf(false) }
    var showPredictionInfo by remember { mutableStateOf(false) }
    var showPaceZonesInfo by remember { mutableStateOf(false) }
    var showVolumeInfo by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val totalDays = try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(userPrefs.raceDate))
    } catch (e: Exception) { 0L }

    // Health Connect state
    val healthConnectManager = remember { HealthConnectManager(context) }
    var hcPermissionsGranted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hcStatus = remember {
        try { HealthConnectClient.getSdkStatus(context) } catch (e: Exception) { -1 }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        hcPermissionsGranted = granted.containsAll(healthConnectManager.requiredPermissions)
        if (hcPermissionsGranted) {
            Toast.makeText(context, "✅ Đã kết nối Health Connect!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        hcPermissionsGranted = healthConnectManager.hasPermissions()
    }

    // Pulse animation for active workout
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLogoDark = when (userPrefs.themeMode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> isSystemInDarkTheme()
                    }
                    val logoResId = if (isLogoDark) com.example.runcoach.R.drawable.app_logo_dark else com.example.runcoach.R.drawable.app_logo_light
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = "RunCoach Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "RunCoach AI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hành trình ${userPrefs.targetDistance}km",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Health Connect Sync
                    IconButton(
                        onClick = {
                            if (hcPermissionsGranted) {
                                Toast.makeText(context, "Đang quét dữ liệu Health Connect...", Toast.LENGTH_SHORT).show()
                                viewModel.checkSyncProposed { proposal, error ->
                                    if (error != null) {
                                        syncResultMessage = error
                                    } else if (proposal != null) {
                                        if (proposal.shifts.isNotEmpty()) {
                                            pendingSyncProposal = proposal
                                        } else {
                                            viewModel.applySync(proposal) { msg ->
                                                syncResultMessage = msg
                                            }
                                        }
                                    }
                                }
                            } else {
                                permissionsLauncher.launch(healthConnectManager.requiredPermissions)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Health Connect",
                            tint = if (hcPermissionsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    // Notification settings
                    IconButton(
                        onClick = { showNotifSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    // Settings / Reset
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Countdown + VDOT card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRaceDayInfo = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Race Day: ${userPrefs.raceDate}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Text("Còn $totalDays ngày 🏁", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = {
                                val total = try {
                                    ChronoUnit.DAYS.between(LocalDate.now().minusDays(workoutsList.size.toLong()), LocalDate.parse(userPrefs.raceDate)).toFloat()
                                } catch (e: Exception) { 1f }
                                val completed = workoutsList.count { it.isCompleted }.toFloat()
                                if (total > 0) (completed / total).coerceIn(0f, 1f) else 0f
                            },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        val completedCount = workoutsList.count { it.isCompleted }
                        val totalCount = workoutsList.count { it.type !in listOf("REST") }
                        Text("$completedCount / $totalCount buổi hoàn thành", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VDOT", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Text(String.format("%.1f", userPrefs.vdotScore), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Prediction card
        item {
            val adherence = remember(workoutsList) {
                val pastWorkouts = workoutsList.filter {
                    try { LocalDate.parse(it.date).isBefore(LocalDate.now()) } catch(e: Exception) { false }
                }
                if (pastWorkouts.isEmpty()) 1.0
                else {
                    val completed = pastWorkouts.count { it.isCompleted }.toDouble()
                    val running = pastWorkouts.count { it.type !in listOf("REST") }.toDouble()
                    if (running > 0) completed / running else 1.0
                }
            }

            val vdotEff = userPrefs.vdotScore.toDouble() * (0.7 + 0.3 * adherence)
            val predictedSeconds = VdotCalculator.predictRaceTime(vdotEff, userPrefs.targetDistance.toDouble())

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPredictionInfo = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔮 Dự đoán thành tích Race Day", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Thời gian mục tiêu (${userPrefs.targetDistance}km)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = VdotCalculator.formatDuration(predictedSeconds),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val predictedPaceSec = if (userPrefs.targetDistance > 0) (predictedSeconds / userPrefs.targetDistance).toInt() else 0
                            if (predictedPaceSec > 0) {
                                Text(
                                    text = "Pace: ${VdotCalculator.formatPace(predictedPaceSec)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Độ tuân thủ giáo án", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = "${(adherence * 100).toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (adherence > 0.8) ColorCompleted else if (adherence > 0.5) ColorWarning else ColorSkipped
                            )
                        }
                    }
                    if (adherence < 0.9) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "💡 Tăng tỉ lệ hoàn thành bài tập để đạt thời gian chạy tốt nhất!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Pace zones
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPaceZonesInfo = true }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Vùng Pace cá nhân hóa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PaceChip("🟢 Easy", VdotCalculator.formatPace(userPrefs.easyPaceSec), ColorEasy)
                        PaceChip("🔵 Long", VdotCalculator.formatPace(userPrefs.longPaceSec), ColorLong)
                        PaceChip("🔴 Tempo", VdotCalculator.formatPace(userPrefs.tempoPaceSec), ColorTempo)
                    }
                }
            }
        }

        // Today's workout
        item {
            Text("Bài tập hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))

            val workout = todayWorkout
            if (workout != null) {
                val wColor = workoutTypeColor(workout)
                val isActiveWorkout = workout.type !in listOf("REST", "CT") && !workout.isCompleted

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (workout.isCompleted) ColorCompleted.copy(alpha = 0.08f)
                        else wColor.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, wColor.copy(alpha = if (isActiveWorkout) 0.5f else 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWorkoutDetails = workout }
                        .then(if (isActiveWorkout) Modifier.scale(pulseScale) else Modifier)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(wColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(workoutTypeIcon(workout), contentDescription = null, tint = wColor, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(wColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(workout.type, color = wColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (workout.isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorCompleted, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hoàn thành", color = ColorCompleted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (workout.isSkipped) {
                                Text("Đã dời lịch", color = ColorSkipped, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(workout.description, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        if (workout.targetDistanceKm > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "🎯 Mục tiêu: Cự ly ${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km - Pace: ${VdotCalculator.formatPace(workout.targetPaceSec)}",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(workout.instructions, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                        if (workout.isCompleted) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                "✅ Thực tế: ${String.format(java.util.Locale.US, "%.1f", workout.actualDistanceKm)} km trong ${workout.actualDurationMin.toInt()} phút (${workout.syncSource})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCompleted
                            )
                        } else if (workout.type !in listOf("REST")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        logDistanceText = ""
                                        logDurationText = ""
                                        selectedRpe = 3
                                        showLogDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = wColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Hoàn thành", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "Đang kiểm tra dữ liệu Health Connect...", Toast.LENGTH_SHORT).show()
                                        viewModel.syncTodayWorkout { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đồng bộ", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("😌 Hôm nay là ngày nghỉ ngoài lịch tập.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.clickable { showVolumeInfo = true }) {
                WeeklyVolumeChart(workoutsList = workoutsList)
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Phiên bản 1.4",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                )
            }
        }
    }

    // Log workout dialog
    val workout = todayWorkout
    if (showLogDialog && workout != null) {
        Dialog(
            onDismissRequest = { showLogDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Ghi nhận bài chạy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Nhập kết quả thực tế bài tập hôm nay",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Target Workout Summary Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mục tiêu: Cự ly ${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km - Pace: ${VdotCalculator.formatPace(workout.targetPaceSec)}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Input: Distance
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = logDistanceText,
                            onValueChange = { logDistanceText = it },
                            label = { Text("Quãng đường thực tế (km)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            },
                            placeholder = { Text("Ví dụ: ${workout.targetDistanceKm}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val current = logDistanceText.toDoubleOrNull() ?: 0.0
                                    val newVal = (current - 0.1).coerceAtLeast(0.0)
                                    logDistanceText = String.format(java.util.Locale.US, "%.1f", newVal)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.1km", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    logDistanceText = workout.targetDistanceKm.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Đúng mục tiêu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val current = logDistanceText.toDoubleOrNull() ?: 0.0
                                    logDistanceText = String.format(java.util.Locale.US, "%.1f", current + 0.1)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.1km", fontSize = 11.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Input: Duration
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = logDurationText,
                            onValueChange = { logDurationText = it },
                            label = { Text("Thời gian chạy thực tế (phút)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            },
                            placeholder = {
                                val estMin = ((workout.targetDistanceKm * workout.targetPaceSec) / 60.0).toInt()
                                Text("Ví dụ: $estMin")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val current = logDurationText.toIntOrNull() ?: 0
                                    val newVal = (current - 1).coerceAtLeast(0)
                                    logDurationText = newVal.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-1 phút", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val targetDurationMin = ((workout.targetDistanceKm * workout.targetPaceSec) / 60.0).toInt()
                                    logDurationText = targetDurationMin.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Đúng mục tiêu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val current = logDurationText.toIntOrNull() ?: 0
                                    logDurationText = (current + 1).toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+1 phút", fontSize = 11.sp)
                            }
                        }
                    }
                    
                    // Real-time Pace Calculation Feedback
                    val dist = logDistanceText.toDoubleOrNull() ?: 0.0
                    val dur = logDurationText.toDoubleOrNull() ?: 0.0
                    if (dist > 0.0 && dur > 0.0) {
                        val paceSec = (dur * 60) / dist
                        val paceStr = VdotCalculator.formatPace(paceSec.toInt())
                        
                        val speedDiff = paceSec - workout.targetPaceSec
                        val (paceMsg, paceColor) = when {
                            speedDiff < -15 -> "Nhanh hơn mục tiêu khá nhiều! Hãy cẩn thận giữ sức. 🔥" to ColorTempo
                            speedDiff > 25 -> "Chạy thả lỏng, nhịp tim an toàn tốt. 🐢" to ColorEasy
                            else -> "Nhịp độ lý tưởng, bám rất sát mục tiêu! 🎯" to ColorCompleted
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = paceColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, paceColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = paceColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pace trung bình thực tế: $paceStr",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = paceColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = paceMsg,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = paceColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // RPE Rating (Perceived Effort)
                    Text(
                        text = "Mức độ gắng sức cảm nhận (RPE)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val rpeOptions = listOf(
                        1 to ("😊" to "Nhẹ nhàng"),
                        2 to ("🏃‍♂️" to "Thoải mái"),
                        3 to ("👍" to "Vừa sức"),
                        4 to ("🥵" to "Mệt mỏi"),
                        5 to ("☠️" to "Kiệt sức")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rpeOptions.forEach { (rating, pair) ->
                            val (emoji, label) = pair
                            val isSelected = selectedRpe == rating
                            
                            val btnBg = if (isSelected) {
                                when(rating) {
                                    1, 2 -> ColorEasy.copy(alpha = 0.15f)
                                    3 -> ColorCompleted.copy(alpha = 0.15f)
                                    4 -> ColorWarning.copy(alpha = 0.15f)
                                    else -> ColorLong.copy(alpha = 0.15f)
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            }
                            
                            val btnBorderColor = if (isSelected) {
                                when(rating) {
                                    1, 2 -> ColorEasy
                                    3 -> ColorCompleted
                                    4 -> ColorWarning
                                    else -> ColorLong
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            }
                            
                            val textColor = if (isSelected) {
                                when(rating) {
                                    1, 2 -> ColorEasy
                                    3 -> ColorCompleted
                                    4 -> ColorWarning
                                    else -> ColorLong
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(btnBg)
                                    .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp))
                                    .clickable { selectedRpe = rating }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Hủy", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val distance = logDistanceText.toDoubleOrNull() ?: 0.0
                                val duration = logDurationText.toDoubleOrNull() ?: 0.0
                                if (distance <= 0.0 || duration <= 0.0) {
                                    Toast.makeText(context, "Nhập quãng đường và thời gian hợp lệ!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.markWorkoutCompletedManually(LocalDate.now().toString(), distance, duration)
                                showLogDialog = false
                                Toast.makeText(context, "Đã ghi nhận bài chạy! 🎉", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Lưu kết quả", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ColorWarning) },
            title = { Text("Đặt lại ứng dụng?") },
            text = { Text("Toàn bộ lịch tập và tiến trình sẽ bị xóa. Ứng dụng sẽ tự động đóng và khởi động lại để áp dụng.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApp {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorWarning)
                ) { Text("Xóa & Khởi động lại") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) { Text("Hủy") }
            }
        )
    }

    // Notification settings dialog
    if (showNotifSettings) {
        NotificationSettingsDialog(
            userPrefs = userPrefs,
            onDismiss = { showNotifSettings = false },
            onSave = { enabled, hour, minute ->
                viewModel.setNotificationsEnabled(enabled)
                viewModel.setNotificationTime(hour, minute)
                showNotifSettings = false
                Toast.makeText(context, "Đã lưu cài đặt thông báo!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Workout details dialog
    if (showWorkoutDetails != null) {
        WorkoutDetailsDialog(
            workout = showWorkoutDetails!!,
            onDismiss = { showWorkoutDetails = null }
        )
    }

    // Sync result dialog
    if (syncResultMessage != null) {
        AlertDialog(
            onDismissRequest = { syncResultMessage = null },
            icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Đồng bộ Health Connect") },
            text = { Text(syncResultMessage!!) },
            confirmButton = {
                Button(onClick = { syncResultMessage = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Sync Confirmation Dialog for Shifts/Swaps
    if (pendingSyncProposal != null) {
        val proposal = pendingSyncProposal!!
        Dialog(
            onDismissRequest = { pendingSyncProposal = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Đồng Bộ & Thay Đổi Lịch",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Phát hiện bạn đã tập chạy khác ngày dự kiến. Bạn có đồng ý cập nhật kết quả và tự động điều chỉnh lịch tập dưới đây?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // List of shifts/swaps
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        proposal.shifts.forEach { shift ->
                            val origDateFormatted = try {
                                val d = LocalDate.parse(shift.originalDate)
                                val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                                "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
                            } catch (e: Exception) { shift.originalDate }

                            val newDateFormatted = try {
                                val d = LocalDate.parse(shift.newDate)
                                val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                                "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
                            } catch (e: Exception) { shift.newDate }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Row showing Shift
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Original planned card
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Lịch dự kiến",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = origDateFormatted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = shift.workout.description,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Chuyển sang",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(18.dp)
                                        )

                                        // Actual run date card
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Đã chạy thực tế",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = newDateFormatted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                            Text(
                                                text = "${String.format(java.util.Locale.US, "%.1f", shift.actualDistanceKm)} km hoàn thành",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // If swap occurred
                                    if (shift.workoutOnNewDate != null) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = "Swap",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            val swapType = if (shift.workoutOnNewDate.type == "REST") "Nghỉ ngơi" else shift.workoutOnNewDate.description
                                            Text(
                                                text = "Bài tập '$swapType' của ngày $newDateFormatted sẽ chuyển sang ngày $origDateFormatted",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Also list exact matches briefly for transparency
                        if (proposal.exactMatches.isNotEmpty()) {
                            Text(
                                text = "Các bài tập khớp đúng lịch:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            proposal.exactMatches.forEach { match ->
                                val dateFormatted = try {
                                    val d = LocalDate.parse(match.workout.date)
                                    val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                                    "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
                                } catch (e: Exception) { match.workout.date }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Khớp",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$dateFormatted: ${match.workout.description} (${String.format(java.util.Locale.US, "%.1f", match.actualDistanceKm)} km)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingSyncProposal = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Hủy bỏ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                val currentProposal = pendingSyncProposal!!
                                pendingSyncProposal = null
                                viewModel.applySync(currentProposal) { msg ->
                                    syncResultMessage = msg
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đồng ý",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Informational Popups
    if (showRaceDayInfo) {
        val totalWorkouts = workoutsList.count { it.type != "REST" && it.type != "CT" }
        val totalDistance = workoutsList.sumOf { it.targetDistanceKm.toDouble() }
        val weeksCount = if (workoutsList.isNotEmpty()) workoutsList.size / 7 else 0

        Dialog(
            onDismissRequest = { showRaceDayInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
                ) {
                    Text("🏁 Mục tiêu Race Day", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tổng quan giáo án của bạn:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Text("• Ngày đua: ${userPrefs.raceDate}", fontSize = 14.sp)
                            Text("• Cự ly mục tiêu: ${userPrefs.targetDistance} km", fontSize = 14.sp)
                            Text("• Tổng thời lượng: $weeksCount tuần", fontSize = 14.sp)
                            Text("• Số buổi chạy: $totalWorkouts buổi", fontSize = 14.sp)
                            Text("• Tổng quãng đường dự kiến: ${String.format(java.util.Locale.US, "%.1f", totalDistance)} km", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "VDOT là gì?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "VDOT viết tắt của V̇O₂max (Volume of Oxygen Maximum - Tốc độ tiêu thụ oxy tối đa). Đây là hệ thống đánh giá thể lực do HLV huyền thoại Jack Daniels phát triển. Nó hoạt động như một hệ thống điểm số chung: VDOT càng cao, khả năng chạy bộ của bạn càng xuất sắc. Ứng dụng dùng VDOT của bạn để tự động tạo ra giáo án phù hợp nhất.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Giáo án này được AI thiết kế theo chuẩn Jack Daniels' Running Formula, tự động chia thành 4 giai đoạn huấn luyện (Base, Build, Peak, Taper) dựa trên số ngày còn lại. Cấu trúc này giúp bạn xây dựng nền tảng vững chắc và đạt điểm rơi phong độ (Peak) tốt nhất vào đúng ngày đua.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showRaceDayInfo = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Đóng", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPredictionInfo) {
        val adherence = remember(workoutsList) {
            val pastWorkouts = workoutsList.filter {
                try { LocalDate.parse(it.date).isBefore(LocalDate.now()) } catch(e: Exception) { false }
            }
            if (pastWorkouts.isEmpty()) 1.0 else {
                val completed = pastWorkouts.count { it.isCompleted }.toDouble()
                val running = pastWorkouts.count { it.type !in listOf("REST") }.toDouble()
                if (running > 0) completed / running else 1.0
            }
        }
        val vdotEff = userPrefs.vdotScore.toDouble() * (0.7 + 0.3 * adherence)
        val predictedSeconds = VdotCalculator.predictRaceTime(vdotEff, userPrefs.targetDistance.toDouble())

        Dialog(
            onDismissRequest = { showPredictionInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text("🔮 Dự đoán thành tích", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Chỉ số hiện tại của bạn:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Text("• VDOT Gốc (Từ bài test): ${String.format(java.util.Locale.US, "%.1f", userPrefs.vdotScore)}", fontSize = 14.sp)
                            Text("• Tỷ lệ tuân thủ giáo án: ${(adherence * 100).toInt()}%", fontSize = 14.sp)
                            Text("• VDOT Thực tế (Đã hiệu chỉnh): ${String.format(java.util.Locale.US, "%.1f", vdotEff)}", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dự đoán thành tích được tính toán bằng cách kết hợp sức mạnh cốt lõi (chỉ số VDOT) và sự chăm chỉ của bạn (Tỷ lệ hoàn thành bài tập).\n\nThuật toán cho thấy với phong độ hiện tại, bạn có thể hoàn thành ${userPrefs.targetDistance}km trong thời gian ${VdotCalculator.formatDuration(predictedSeconds)} (Pace trung bình ${VdotCalculator.formatPace((predictedSeconds / userPrefs.targetDistance).toInt())}).\n\n💡 Lời khuyên: Hãy bám sát giáo án và không bỏ lỡ các bài tập Long Run cuối tuần để nâng cao tỷ lệ tuân thủ, từ đó rút ngắn thời gian dự đoán!",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showPredictionInfo = false }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Đóng", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }

    if (showPaceZonesInfo) {
        val easyCount = workoutsList.count { it.type == "EASY" }
        val longCount = workoutsList.count { it.type == "LONG" }
        val tempoCount = workoutsList.count { it.type == "TEMPO" || it.type == "INTERVAL" }
        val runningCount = easyCount + longCount + tempoCount

        Dialog(
            onDismissRequest = { showPaceZonesInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text("📊 Vùng Pace & Phân bổ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("Tốc độ (Pace) được thiết kế chuyên biệt dựa trên VDOT của bạn, phân bổ theo quy tắc 80/20 (80% bài tập nhẹ nhàng, 20% bài tập cường độ cao).", fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val pctEasy = if (runningCount > 0) (easyCount + longCount) * 100 / runningCount else 0
                    val pctTempo = if (runningCount > 0) tempoCount * 100 / runningCount else 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorEasy.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🟢 Easy Pace (${VdotCalculator.formatPace(userPrefs.easyPaceSec)})", fontWeight = FontWeight.Bold, color = ColorEasy)
                            Text("Chạy thả lỏng, nhịp tim vùng 2. Xây dựng sức bền hiếu khí nền tảng.", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Trong giáo án: $easyCount buổi (Đóng góp vào $pctEasy% khối lượng nhẹ)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorLong.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🔵 Long Run Pace (${VdotCalculator.formatPace(userPrefs.longPaceSec)})", fontWeight = FontWeight.Bold, color = ColorLong)
                            Text("Bài chạy dài cuối tuần. Giúp cơ thể làm quen với việc đốt mỡ và sức chịu đựng cơ bắp.", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Trong giáo án: $longCount buổi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorTempo.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🔴 Tempo/Race Pace (${VdotCalculator.formatPace(userPrefs.tempoPaceSec)})", fontWeight = FontWeight.Bold, color = ColorTempo)
                            Text("Chạy ở ngưỡng yếm khí (Lactate Threshold). Dạy cơ thể chạy nhanh hơn mà không bị mỏi.", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Trong giáo án: $tempoCount buổi (Chiếm $pctTempo% cường độ cao)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showPaceZonesInfo = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Đóng", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showVolumeInfo) {
        val weeklyVols = workoutsList.groupBy { 
            val d = try { LocalDate.parse(it.date) } catch(e:Exception) { LocalDate.now() }
            ChronoUnit.WEEKS.between(LocalDate.parse(workoutsList.first().date), d)
        }.mapValues { it.value.sumOf { w -> w.targetDistanceKm.toDouble() } }
        
        val maxVolWeek = weeklyVols.maxByOrNull { it.value }
        val maxVol = maxVolWeek?.value ?: 0.0
        val totalVol = weeklyVols.values.sum()

        Dialog(
            onDismissRequest = { showVolumeInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text("📈 Khối lượng luyện tập", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Phân tích dữ liệu:", fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Text("• Khối lượng tích lũy: ${String.format(java.util.Locale.US, "%.1f", totalVol)} km", fontSize = 14.sp)
                            Text("• Tuần đạt đỉnh (Peak): Tuần thứ ${(maxVolWeek?.key ?: 0) + 1}", fontSize = 14.sp)
                            Text("• Quãng đường tuần đỉnh: ${String.format(java.util.Locale.US, "%.1f", maxVol)} km", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Biểu đồ khối lượng (Mileage) cho thấy nguyên tắc tăng tiến an toàn (không tăng quá 10% mỗi tuần).\n\nKhi bước vào 1-2 tuần cuối cùng trước Race Day, bạn sẽ thấy cột biểu đồ sụt giảm đột ngột. Đây là kỹ thuật Tapering (giảm tải) cực kỳ quan trọng, giúp cơ bắp đào thải toàn bộ mệt mỏi tích tụ và nạp đầy năng lượng Glycogen chuẩn bị cho ngày thi đấu.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showVolumeInfo = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Đóng", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaceChip(label: String, pace: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(pace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun HealthConnectCard(
    hcStatus: Int,
    hcPermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onSync: () -> Unit
) {
    var showGuide by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                hcPermissionsGranted -> Color(0xFF4285F4).copy(alpha = 0.08f)
                hcStatus == HealthConnectClient.SDK_AVAILABLE -> ColorWarning.copy(alpha = 0.06f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, when {
            hcPermissionsGranted -> Color(0xFF4285F4).copy(alpha = 0.3f)
            hcStatus == HealthConnectClient.SDK_AVAILABLE -> ColorWarning.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        }),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (hcPermissionsGranted) Color(0xFF4285F4).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hcPermissionsGranted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (hcPermissionsGranted) Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Health Connect",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        when {
                            hcPermissionsGranted -> "✅ Đã kết nối và có quyền đọc"
                            hcStatus == HealthConnectClient.SDK_AVAILABLE -> "⚠️ Chưa cấp quyền — nhấn để kết nối"
                            hcStatus == HealthConnectClient.SDK_UNAVAILABLE -> "❌ Không khả dụng trên thiết bị này"
                            else -> "📥 Cần cài đặt Health Connect"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Help Icon to toggle guide
                    IconButton(
                        onClick = { showGuide = !showGuide },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showGuide) Icons.Default.ExpandLess else Icons.Default.HelpOutline,
                            contentDescription = "Show guide",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (hcStatus == HealthConnectClient.SDK_AVAILABLE) {
                        if (hcPermissionsGranted) {
                            IconButton(onClick = onSync, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color(0xFF4285F4))
                            }
                        } else {
                            TextButton(onClick = onRequestPermissions, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Kết nối", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showGuide,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "📖 Hướng Dẫn Kết Nối Health Connect",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Yêu cầu thiết bị:\n" +
                                "• Android 10 trở lên.\n" +
                                "• Đã cài đặt ứng dụng Health Connect từ Google Play Store (nếu chạy Android 13 trở xuống).\n\n" +
                                "2. Các bước liên kết:\n" +
                                "• Bước 1: Nhấp vào nút 'Kết nối' ở góc phải.\n" +
                                "• Bước 2: Bật tất cả các quyền (đặc biệt là 'Bài tập/Exercise Session' và 'Quãng đường/Distance') khi hệ thống hiển thị bảng cấp quyền.\n" +
                                "• Bước 3: Mở ứng dụng thể thao ưa thích của bạn (Garmin Connect, Strava, Nike Run Club, Wahoo...).\n" +
                                "• Bước 4: Vào Cài đặt trong ứng dụng đó và bật liên kết gửi dữ liệu sang Health Connect.\n\n" +
                                "3. Sử dụng:\n" +
                                "• Sau khi bạn chạy xong và đồng bộ lên Strava/Garmin, RunCoach AI sẽ tự động đọc dữ liệu chạy thực tế qua Health Connect khi bạn bấm nút 'Đồng bộ'.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )

                    if (hcStatus != HealthConnectClient.SDK_AVAILABLE && hcStatus != HealthConnectClient.SDK_UNAVAILABLE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val contextLocal = LocalContext.current
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    contextLocal.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                    }
                                    contextLocal.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tải Health Connect trên CH Play", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyVolumeChart(workoutsList: List<WorkoutEntity>) {
    var showAnalyticsDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showAnalyticsDialog = true }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Khối lượng tập luyện theo tuần",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Xem chi tiết",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val weeklyStats = remember(workoutsList) {
                workoutsList.groupBy { it.weekNumber }.map { (week, list) ->
                    val target = list.count { it.type != "REST" }.toDouble()
                    val actual = list.count { it.type != "REST" && it.isCompleted }.toDouble()
                    week to Pair(target, actual)
                }.sortedBy { it.first }
            }

            if (weeklyStats.isNotEmpty()) {
                val maxVolume = weeklyStats.maxOfOrNull { maxOf(it.second.first, it.second.second) }?.coerceAtLeast(1.0) ?: 1.0

                Box(modifier = Modifier.fillMaxWidth().height(140.dp).horizontalScroll(rememberScrollState())) {
                    val barWidth = 28.dp
                    val spacing = 12.dp
                    val totalW = (barWidth + spacing) * weeklyStats.size + spacing

                    Canvas(modifier = Modifier.width(totalW).fillMaxHeight().padding(vertical = 8.dp)) {
                        val canvasH = size.height
                        val bwPx = barWidth.toPx()
                        val spPx = spacing.toPx()

                        val paint = android.graphics.Paint().apply {
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        weeklyStats.forEachIndexed { idx, (week, stats) ->
                            val (target, actual) = stats
                            val x = spPx + idx * (bwPx + spPx)

                            // Target bar
                            val tH = (target / maxVolume * (canvasH - 36f)).toFloat()
                            drawRoundRect(
                                color = Color.Gray.copy(alpha = 0.15f),
                                topLeft = Offset(x, canvasH - 36f - tH),
                                size = Size(bwPx, tH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            // Actual bar
                            if (actual > 0.0) {
                                val aH = (actual / maxVolume * (canvasH - 36f)).toFloat()
                                drawRoundRect(
                                    color = DarkPrimary,
                                    topLeft = Offset(x, canvasH - 36f - aH),
                                    size = Size(bwPx, aH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }

                            // Week label
                            drawIntoCanvas { canvas ->
                                paint.color = if (actual >= target && target > 0)
                                    android.graphics.Color.parseColor("#A3E635")
                                else android.graphics.Color.parseColor("#8E8E93")
                                canvas.nativeCanvas.drawText("T$week", x + bwPx / 2, canvasH - 6f, paint)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mục tiêu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(DarkPrimary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thực tế", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có dữ liệu biểu đồ tuần.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showAnalyticsDialog) {
        DetailedAnalyticsDialog(
            workoutsList = workoutsList,
            onDismiss = { showAnalyticsDialog = false }
        )
    }
}

// ─────────────────────────────────────────────
// DETAILED ANALYTICS POPUP & CHARTS
// ─────────────────────────────────────────────

data class AnalyticsChartItem(
    val label: String,
    val target: Double,
    val actual: Double,
    val workouts: List<WorkoutEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnalyticsDialog(
    workoutsList: List<WorkoutEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBarIndex by remember { mutableIntStateOf(-1) }
    var showChartTip by remember { mutableStateOf(true) }

    // Reset selected bar when tab changes
    LaunchedEffect(selectedTab) {
        selectedBarIndex = -1
    }

    // Group workouts based on tab selection
    val chartItems = remember(workoutsList, selectedTab) {
        when (selectedTab) {
            0 -> { // Week tab
                workoutsList.groupBy { it.weekNumber }
                    .map { (week, list) ->
                        val target = list.count { it.type != "REST" }.toDouble()
                        val actual = list.count { it.type != "REST" && it.isCompleted }.toDouble()
                        AnalyticsChartItem(
                            label = "Tuần $week",
                            target = target,
                            actual = actual,
                            workouts = list
                        )
                    }.sortedBy { 
                        it.label.substringAfter("Tuần ").toIntOrNull() ?: 0 
                    }
            }
            1 -> { // Month tab
                workoutsList.groupBy { workout ->
                    val date = try { LocalDate.parse(workout.date) } catch (e: Exception) { null }
                    if (date != null) {
                        "${date.year}-${String.format(java.util.Locale.US, "%02d", date.monthValue)}"
                    } else {
                        "Unknown"
                    }
                }.filterKeys { it != "Unknown" }
                 .toList()
                 .sortedBy { it.first }
                 .map { (monthKey, list) ->
                     val parts = monthKey.split("-")
                     val year = parts[0].toInt()
                     val month = parts[1].toInt()
                     val label = "Th $month/${year % 100}"
                     val target = list.sumOf { it.targetDistanceKm }
                     val actual = list.sumOf { it.actualDistanceKm }
                     AnalyticsChartItem(
                         label = label,
                         target = target,
                         actual = actual,
                         workouts = list
                     )
                 }
            }
            else -> { // Year tab
                workoutsList.groupBy { workout ->
                    val date = try { LocalDate.parse(workout.date) } catch (e: Exception) { null }
                    date?.year?.toString() ?: "Unknown"
                }.filterKeys { it != "Unknown" }
                 .toList()
                 .sortedBy { it.first }
                 .map { (yearStr, list) ->
                     val label = "Năm $yearStr"
                     val target = list.sumOf { it.targetDistanceKm }
                     val actual = list.sumOf { it.actualDistanceKm }
                     AnalyticsChartItem(
                         label = label,
                         target = target,
                         actual = actual,
                         workouts = list
                     )
                 }
            }
        }
    }

    // Active workouts list for stats card (all workouts in tab or specific selected bar)
    val activeWorkouts = remember(chartItems, selectedBarIndex) {
        if (selectedBarIndex != -1 && selectedBarIndex < chartItems.size) {
            chartItems[selectedBarIndex].workouts
        } else {
            chartItems.flatMap { it.workouts }
        }
    }

    val targetSum = remember(activeWorkouts) { activeWorkouts.sumOf { it.targetDistanceKm } }
    val actualSum = remember(activeWorkouts) { activeWorkouts.sumOf { it.actualDistanceKm } }
    val ratio = remember(targetSum, actualSum) {
        if (targetSum > 0.0) (actualSum / targetSum * 100.0) else 0.0
    }
    val completedCount = remember(activeWorkouts) { activeWorkouts.count { it.isCompleted } }
    val totalCount = remember(activeWorkouts) { activeWorkouts.count { it.type != "REST" } }

    val totalDurationMin = remember(activeWorkouts) { activeWorkouts.sumOf { it.actualDurationMin } }
    val avgPace = remember(actualSum, totalDurationMin) {
        calculateAveragePace(actualSum, totalDurationMin)
    }

    // Coach feedback specific evaluation (only including workouts up to today for overall view)
    val todayStr = remember { LocalDate.now().toString() }
    val feedbackWorkouts = remember(activeWorkouts, selectedBarIndex, todayStr) {
        if (selectedBarIndex == -1) {
            activeWorkouts.filter { it.date <= todayStr }
        } else {
            activeWorkouts
        }
    }

    val feedbackTargetSum = remember(feedbackWorkouts) { feedbackWorkouts.sumOf { it.targetDistanceKm } }
    val feedbackActualSum = remember(feedbackWorkouts) { feedbackWorkouts.sumOf { it.actualDistanceKm } }
    val feedbackRatio = remember(feedbackTargetSum, feedbackActualSum) {
        if (feedbackTargetSum > 0.0) (feedbackActualSum / feedbackTargetSum * 100.0) else 0.0
    }
    val feedbackCompletedCount = remember(feedbackWorkouts) { feedbackWorkouts.count { it.isCompleted } }
    val feedbackTotalCount = remember(feedbackWorkouts) { feedbackWorkouts.count { it.type != "REST" } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phân tích Luyện tập",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                PillTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    tabs = listOf("Tuần", "Tháng", "Năm")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Indicator Banner
                if (selectedBarIndex != -1 && selectedBarIndex < chartItems.size) {
                    val selectedItem = chartItems[selectedBarIndex]
                    val completionRate = if (selectedItem.target > 0.0) (selectedItem.actual / selectedItem.target * 100.0) else 0.0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${selectedItem.label}: Thực tế ${String.format(java.util.Locale.US, "%.1f", selectedItem.actual)}km / Kế hoạch ${String.format(java.util.Locale.US, "%.1f", selectedItem.target)}km (${String.format(java.util.Locale.US, "%.1f", completionRate)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (showChartTip) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 Chạm vào cột biểu đồ để xem chi tiết từng giai đoạn.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(
                                onClick = { showChartTip = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tắt gợi ý",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas Chart
                DetailedBarChart(
                    items = chartItems,
                    selectedIndex = selectedBarIndex,
                    unit = if (selectedTab == 0) "buổi" else "km",
                    onBarSelected = { selectedBarIndex = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mục tiêu",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thực tế",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Detailed Metrics Title
                Text(
                    text = if (selectedBarIndex != -1) "Số liệu ${chartItems[selectedBarIndex].label}" else "Số liệu tổng quan giai đoạn",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Metrics Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnalyticsMetricCard(
                            title = "TỔNG CỰ LY",
                            value = "${String.format(java.util.Locale.US, "%.1f", actualSum)} km",
                            subtitle = "Mục tiêu: ${String.format(java.util.Locale.US, "%.1f", targetSum)} km",
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsMetricCard(
                            title = "TỶ LỆ HOÀN THÀNH",
                            value = "${String.format(java.util.Locale.US, "%.1f", ratio)}%",
                            subtitle = when {
                                ratio >= 90.0 -> "Xuất sắc! 🔥"
                                ratio >= 70.0 -> "Khá tốt! 👍"
                                ratio > 0.0 -> "Cần cố gắng 🏃‍♂️"
                                else -> "Chưa chạy bài nào 💤"
                            },
                            icon = Icons.Default.CheckCircle,
                            iconTint = if (ratio >= 90.0) ColorCompleted else if (ratio >= 70.0) ColorCompleted.copy(alpha = 0.7f) else ColorWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnalyticsMetricCard(
                            title = "SỐ BUỔI CHẠY",
                            value = "$completedCount buổi",
                            subtitle = "Kế hoạch: $totalCount buổi",
                            icon = Icons.Default.DateRange,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsMetricCard(
                            title = "PACE TRUNG BÌNH",
                            value = "$avgPace /km",
                            subtitle = "Tốc độ trung bình thực tế",
                            icon = Icons.Default.Timer,
                            iconTint = ColorEasy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Motivational AI Insight
                val comment = when {
                    feedbackTotalCount > 0 && feedbackCompletedCount == feedbackTotalCount -> {
                        "Xuất sắc! Bạn đã hoàn thành đầy đủ tất cả các buổi tập theo kế hoạch của giai đoạn này. Hãy tiếp tục phong độ tuyệt vời này nhé! 🔥"
                    }
                    feedbackRatio >= 95.0 -> {
                        "Phong độ đỉnh cao! Bạn đang bám sát giáo án và hoàn thành cực kỳ xuất sắc mục tiêu đề ra. Hãy tiếp tục duy trì nhé!"
                    }
                    feedbackRatio >= 80.0 -> {
                        "Tuyệt vời! Kết quả luyện tập của bạn rất tích cực. Bạn đang đi đúng hướng để sẵn sàng cho ngày đua."
                    }
                    feedbackRatio >= 50.0 -> {
                        "Khá tốt, tuy nhiên bạn cần chú ý tập luyện đều đặn hơn để đảm bảo không bị quá tải ở các tuần tiếp theo."
                    }
                    feedbackRatio > 0.0 -> {
                        "Lượng luyện tập thực tế đang hơi thấp so với mục tiêu. Hãy thu xếp thời gian hoàn thành các bài chạy Easy nhẹ nhàng nhé."
                    }
                    else -> {
                        "Chưa bắt đầu tập luyện. Hãy xỏ giày vào chạy nhẹ nhàng bài Easy đầu tiên để tạo thói quen tốt ngay hôm nay!"
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Nhận xét huấn luyện viên",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = comment,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PillTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                label = "tabText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun DetailedBarChart(
    items: List<AnalyticsChartItem>,
    selectedIndex: Int,
    unit: String = "km",
    onBarSelected: (Int) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có dữ liệu tập luyện", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        return
    }

    val maxVal = items.maxOfOrNull { maxOf(it.target, it.actual) }?.coerceAtLeast(1.0) ?: 1.0
    val roundedMax = if (unit == "buổi") {
        when {
            maxVal <= 5.0 -> 5.0
            maxVal <= 7.0 -> 7.0
            maxVal <= 10.0 -> 10.0
            else -> maxVal
        }
    } else {
        when {
            maxVal <= 10.0 -> 10.0
            maxVal <= 25.0 -> 25.0
            maxVal <= 50.0 -> 50.0
            maxVal <= 100.0 -> 100.0
            else -> ((maxVal / 50.0).toInt() + 1) * 50.0
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val completedColor = ColorCompleted
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
    ) {
        val totalWidth = maxWidth
        val yAxisWidth = 48.dp
        val graphWidth = totalWidth - yAxisWidth

        val colPreferredWidth = 60.dp
        val scrollableWidth = maxOf(graphWidth, colPreferredWidth * items.size)

        Row(modifier = Modifier.fillMaxSize()) {
            // 1. Static Y-Axis Canvas
            Canvas(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val chartH = canvasH - 32.dp.toPx()

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val fraction = i.toFloat() / gridSteps
                    val y = chartH * (1f - fraction)
                    
                    // Y label
                    val valLabel = "${(roundedMax * fraction).toInt()}"
                    drawIntoCanvas { canvas ->
                        textPaint.color = if (i == 0) android.graphics.Color.LTGRAY else android.graphics.Color.GRAY
                        canvas.nativeCanvas.drawText(
                            valLabel,
                            canvasW - 12f,
                            y + 8f,
                            textPaint
                        )
                    }
                }

                // Draw unit label on Y axis top
                drawIntoCanvas { canvas ->
                    val unitPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    canvas.nativeCanvas.drawText(
                        "($unit)",
                        canvasW - 12f,
                        -14f,
                        unitPaint
                    )
                }
            }

            // 2. Horizontally Scrollable Graph Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .width(scrollableWidth)
                        .fillMaxHeight()
                        .pointerInput(items) {
                            detectTapGestures { offset ->
                                val colWidthPx = size.width / items.size
                                val clickedIdx = (offset.x / colWidthPx).toInt().coerceIn(0, items.size - 1)
                                onBarSelected(if (selectedIndex == clickedIdx) -1 else clickedIdx)
                            }
                        }
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val chartH = canvasH - 32.dp.toPx()

                    // Draw Grid Lines
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val fraction = i.toFloat() / gridSteps
                        val y = chartH * (1f - fraction)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasW, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw Bars
                    val colWidth = canvasW / items.size
                    val barSpacing = colWidth * 0.15f
                    val availableBarWidth = colWidth - barSpacing
                    val barW = (availableBarWidth / 2f).coerceAtMost(16.dp.toPx())

                    items.forEachIndexed { idx, item ->
                        val colCenterX = idx * colWidth + colWidth / 2f

                        // Draw column selection highlight background
                        if (selectedIndex == idx) {
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.08f),
                                topLeft = Offset(idx * colWidth, 0f),
                                size = Size(colWidth, chartH),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }

                        // A. Target Bar (Gray/Translucent primary outline)
                        val targetH = (item.target / roundedMax * chartH).toFloat().coerceAtLeast(2f)
                        val targetLeft = colCenterX - barW - 2f
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.15f),
                            topLeft = Offset(targetLeft, chartH - targetH),
                            size = Size(barW, targetH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // B. Actual Bar (Solid color based on completion)
                        if (item.actual > 0.0) {
                            val actualH = (item.actual / roundedMax * chartH).toFloat().coerceAtLeast(2f)
                            val actualLeft = colCenterX + 2f
                            val actualColor = if (item.actual >= item.target && item.target > 0.0) completedColor else primaryColor

                            drawRoundRect(
                                color = actualColor,
                                topLeft = Offset(actualLeft, chartH - actualH),
                                size = Size(barW, actualH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        // C. X-Axis Labels
                        val displayLabel = if (item.label.startsWith("Tuần ")) {
                            "T${item.label.substringAfter("Tuần ")}"
                        } else {
                            item.label
                        }

                        val labelPaint = android.graphics.Paint().apply {
                            color = if (selectedIndex == idx) primaryColor.toArgb() else android.graphics.Color.GRAY
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                            if (selectedIndex == idx) {
                                isFakeBoldText = true
                            }
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                displayLabel,
                                colCenterX,
                                canvasH - 6f,
                                labelPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

fun calculateAveragePace(totalDistance: Double, totalDurationMin: Double): String {
    if (totalDistance <= 0.0 || totalDurationMin <= 0.0) return "--:--"
    val totalSec = (totalDurationMin * 60.0) / totalDistance
    val min = (totalSec / 60.0).toInt()
    val sec = (totalSec % 60.0).toInt()
    return String.format(java.util.Locale.US, "%d:%02d", min, sec)
}

@Composable
fun NotificationSettingsDialog(
    userPrefs: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, Int) -> Unit
) {
    var enabled by remember { mutableStateOf(userPrefs.isNotificationEnabled) }
    var hour by remember { mutableIntStateOf(userPrefs.notificationHour) }
    var minute by remember { mutableIntStateOf(userPrefs.notificationMinute) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "🔔 Đã cấp quyền thông báo thành công!", Toast.LENGTH_SHORT).show()
            enabled = true
        } else {
            enabled = false
            Toast.makeText(context, "⚠️ Cần cấp quyền thông báo trong cài đặt để nhận nhắc nhở chạy bộ.", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cài đặt thông báo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Toggle
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bật thông báo hàng ngày", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        enabled = true
                                    }
                                } else {
                                    enabled = true
                                }
                            } else {
                                enabled = false
                            }
                        }
                    )
                }

                AnimatedVisibility(visible = enabled) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Giờ nhận thông báo", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Time display button
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    hour = h; minute = m
                                }, hour, minute, true).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                String.format("%02d:%02d mỗi ngày", hour, minute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "💡 Ứng dụng sẽ nhắc nhở bạn về bài tập trong ngày vào đúng giờ này.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("Hủy") }
                    Button(
                        onClick = {
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    Toast.makeText(context, "⚠️ Vui lòng cấp quyền thông báo để lưu cài đặt này.", Toast.LENGTH_SHORT).show()
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@Button
                                }
                            }
                            onSave(enabled, hour, minute)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Lưu") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PLAN SCREEN — Weekly grouped view
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCustomPlan: () -> Unit
) {
    val workoutsList by viewModel.workouts.collectAsState()
    val context = LocalContext.current

    val groupedWorkouts = remember(workoutsList) {
        workoutsList.groupBy { it.weekNumber }.toList().sortedBy { it.first }
    }

    // Reschedule dialog state
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var rescheduleTargetWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var showWorkoutDetails by remember { mutableStateOf<WorkoutEntity?>(null) }

    var showExportMenu by remember { mutableStateOf(false) }

    // Drag and Drop Swap states
    var showSwapConfirmation by remember { mutableStateOf(false) }
    var swapSourceWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var swapTargetWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }

    val dragAndDropState = remember(workoutsList) {
        DragAndDropState(
            onDrop = { source, target ->
                swapSourceWorkout = source
                swapTargetWorkout = target
                showSwapConfirmation = true
            }
        )
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPlanFromCsv(
                context = context,
                uri = uri,
                onSuccess = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    val builder = android.app.AlertDialog.Builder(context)
                    builder.setTitle("Lỗi Nhập File CSV")
                    builder.setMessage(err)
                    builder.setPositiveButton("Đồng ý", null)
                    builder.show()
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Lịch Trình Tập Luyện", fontWeight = FontWeight.Bold)
                            Text("${workoutsList.count { it.isCompleted }} buổi đã hoàn thành", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToCustomPlan) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Thiết kế giáo án",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Chia sẻ",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (showExportMenu) {
                            PlanShareDialog(
                                onDismiss = { showExportMenu = false },
                                onExportCsv = {
                                    viewModel.exportPlanToCsv(context) { uri ->
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Chia sẻ CSV"))
                                        } else {
                                            Toast.makeText(context, "Lỗi khi xuất file CSV!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onExportPdf = {
                                    viewModel.exportPlanToPdf(context) { uri ->
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Chia sẻ PDF"))
                                        } else {
                                            Toast.makeText(context, "Lỗi khi xuất file PDF!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onImportCsv = {
                                    csvPickerLauncher.launch("*/*")
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groupedWorkouts) { (week, dayWorkouts) ->
                    var isExpanded by remember { mutableStateOf(week == 1) }

                    val completedInWeek = dayWorkouts.count { it.isCompleted }
                    val runningWorkouts = dayWorkouts.filter { it.type !in listOf("REST", "CT") }
                    val weekProgress = if (runningWorkouts.isNotEmpty()) completedInWeek.toFloat() / runningWorkouts.size else 0f

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Tuần $week", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        if (completedInWeek > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier.clip(CircleShape).background(ColorCompleted.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("$completedInWeek/${runningWorkouts.size}", fontSize = 10.sp, color = ColorCompleted, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    val totalDist = dayWorkouts.sumOf { it.targetDistanceKm }
                                    Text(
                                        "Tổng: ${String.format("%.1f", totalDist)}km · ${getWeekDateRange(dayWorkouts)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (weekProgress > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { weekProgress },
                                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                            color = ColorCompleted,
                                            trackColor = ColorCompleted.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dayWorkouts.forEach { workout ->
                                        WorkoutDayRow(
                                            workout = workout,
                                            onReschedule = {
                                                rescheduleTargetWorkout = workout
                                                showRescheduleDialog = true
                                            },
                                            onClick = {
                                                showWorkoutDetails = workout
                                            },
                                            dragAndDropState = dragAndDropState,
                                            workoutsList = workoutsList
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dragAndDropState.isDragging && dragAndDropState.dragItem != null) {
            val dragItem = dragAndDropState.dragItem!!
            val offset = dragAndDropState.currentDragPosition - dragAndDropState.localTouchOffset
            val density = LocalDensity.current
            val itemRect = dragAndDropState.itemBounds[dragItem.date]
            val itemWidth = if (itemRect != null) {
                with(density) { itemRect.width.toDp() }
            } else {
                LocalConfiguration.current.screenWidthDp.dp - 32.dp
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                        .width(itemWidth)
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .alpha(1.0f)
                ) {
                    WorkoutDayRow(
                        workout = dragItem,
                        onReschedule = {},
                        onClick = {},
                        isDragging = false
                    )
                }
            }
        }
    }

    // Reschedule dialog
    if (showRescheduleDialog && rescheduleTargetWorkout != null) {
        RescheduleDialog(
            workout = rescheduleTargetWorkout!!,
            onDismiss = { showRescheduleDialog = false },
            onConfirm = { newDate ->
                viewModel.rescheduleWorkout(
                    originalDate = rescheduleTargetWorkout!!.date,
                    newDate = newDate
                ) { result, conflictDesc ->
                    when (result) {
                        0 -> Toast.makeText(context, "✅ Đã dời bài tập sang $newDate", Toast.LENGTH_SHORT).show()
                        1 -> Toast.makeText(context, "⚠️ Đã dời sang $newDate (ngày đó đã có: $conflictDesc)", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, "❌ Không tìm thấy bài tập gốc.", Toast.LENGTH_SHORT).show()
                    }
                }
                showRescheduleDialog = false
            }
        )
    }

    // Swap confirmation dialog
    if (showSwapConfirmation && swapSourceWorkout != null && swapTargetWorkout != null) {
        CustomSwapConfirmationDialog(
            sourceWorkout = swapSourceWorkout!!,
            targetWorkout = swapTargetWorkout!!,
            onConfirmAllWeeks = {
                showSwapConfirmation = false
                viewModel.swapWorkouts(swapSourceWorkout!!, swapTargetWorkout!!, applyToSubsequentWeeks = true)
            },
            onConfirmThisWeekOnly = {
                showSwapConfirmation = false
                viewModel.swapWorkouts(swapSourceWorkout!!, swapTargetWorkout!!, applyToSubsequentWeeks = false)
            },
            onDismiss = {
                showSwapConfirmation = false
            }
        )
    }

    // Workout details dialog
    if (showWorkoutDetails != null) {
        WorkoutDetailsDialog(
            workout = showWorkoutDetails!!,
            onDismiss = { showWorkoutDetails = null }
        )
    }
}

@Composable
fun WorkoutDayRow(
    workout: WorkoutEntity,
    onReschedule: () -> Unit,
    onClick: () -> Unit,
    dragAndDropState: DragAndDropState? = null,
    workoutsList: List<WorkoutEntity> = emptyList(),
    isDragging: Boolean = false
) {
    val wColor = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)
    val isHovered = dragAndDropState?.hoverItem?.date == workout.date

    val dragModifier = if (dragAndDropState != null) {
        Modifier.workoutDragAndDropTarget(workout, dragAndDropState, workoutsList)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    workout.isSkipped -> ColorSkipped.copy(alpha = if (isDragging) 0.15f else 0.05f)
                    workout.isCompleted -> ColorCompleted.copy(alpha = if (isDragging) 0.15f else 0.05f)
                    else -> if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                }
            )
            .border(
                if (isHovered) 2.dp else 1.dp,
                if (isHovered) MaterialTheme.colorScheme.primary else wColor.copy(alpha = if (workout.isSkipped) 0.1f else if (isDragging) 0.6f else 0.15f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator + icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(wColor.copy(alpha = if (workout.isSkipped) 0.1f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (workout.isSkipped) wColor.copy(alpha = 0.4f) else wColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            val dayLabel = try {
                val d = LocalDate.parse(workout.date)
                val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
            } catch (e: Exception) { workout.date }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDragging) 0.8f else 0.5f))
                if (workout.rescheduledFromDate != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ColorRecovery.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text("Dời lịch", fontSize = 9.sp, color = ColorRecovery)
                    }
                }
            }
            Text(
                workout.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (workout.isSkipped) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface
            )
        }

        // Right side: status or distance
        when {
            workout.isSkipped -> {
                Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = ColorSkipped, modifier = Modifier.size(18.dp))
            }
            workout.isCompleted -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${String.format(java.util.Locale.US, "%.1f", workout.actualDistanceKm)} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorCompleted)
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorCompleted, modifier = Modifier.size(16.dp))
                }
            }
            workout.targetDistanceKm > 0 -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDragging) 0.8f else 0.5f))
                    if (workout.type !in listOf("REST", "CT")) {
                        IconButton(onClick = onReschedule, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Dời lịch", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            else -> {
                Text(workout.type, fontSize = 11.sp, color = ColorRest)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleDialog(
    workout: WorkoutEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newDateText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dời lịch tập", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show original workout info
                Card(
                    colors = CardDefaults.cardColors(containerColor = workoutTypeColor(workout).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(workoutTypeIcon(workout), contentDescription = null, tint = workoutTypeColor(workout), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(workout.description, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Ngày gốc: ${workout.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Chuyển sang ngày:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newDateText,
                    onValueChange = { newDateText = it },
                    placeholder = { Text("YYYY-MM-DD") },
                    label = { Text("Ngày mới") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📌 Bài tập gốc sẽ bị đánh dấu SKIPPED. Kế hoạch tổng thể không thay đổi.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("Hủy") }
                    Button(
                        onClick = {
                            try {
                                val parsed = LocalDate.parse(newDateText)
                                if (parsed.isBefore(LocalDate.now())) {
                                    Toast.makeText(context, "Không thể dời về ngày đã qua!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onConfirm(newDateText)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Định dạng ngày không hợp lệ (YYYY-MM-DD)!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Xác nhận") }
                }
            }
        }
    }
}

// Utility: get human-readable date range for a week's workouts
fun getWeekDateRange(workouts: List<WorkoutEntity>): String {
    if (workouts.isEmpty()) return ""
    return try {
        val dates = workouts.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        val first = dates.minOrNull()
        val last = dates.maxOrNull()
        if (first != null && last != null) "${first.dayOfMonth}/${first.monthValue} – ${last.dayOfMonth}/${last.monthValue}"
        else ""
    } catch (e: Exception) { "" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsDialog(
    workout: WorkoutEntity,
    onDismiss: () -> Unit
) {
    val color = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)
    
    // Warmup & Cooldown times based on workout type
    val (warmupTime, warmupText) = when (workout.type) {
        "EASY" -> 8 to "5 phút đi bộ khởi động + 3 phút bài xoay khớp linh hoạt (hông, gối, cổ chân)."
        "LONG" -> 12 to "8 phút chạy nhẹ khởi động chậm + 4 phút dãn cơ động (dynamic stretching) và đá chân."
        "TEMPO" -> 15 to "10 phút chạy bộ nhẹ nhàng + 5 phút khởi động chuyên sâu (dynamic strides, nâng cao đùi)."
        "RECOVERY" -> 3 to "3 phút đi bộ thả lỏng khớp."
        "CT" -> 5 to "5 phút xoay các khớp và kéo giãn toàn thân nhẹ nhàng."
        else -> 0 to "Không cần khởi động."
    }
    
    val (cooldownTime, cooldownText) = when (workout.type) {
        "EASY" -> 10 to "5 phút đi bộ chậm hạ tim + 5 phút căng cơ tĩnh (static stretching) bắp chân, đùi, hông."
        "LONG" -> 15 to "8 phút đi bộ thả lỏng hoàn toàn + 7 phút dãn cơ tĩnh sâu để tránh căng mỏi hôm sau."
        "TEMPO" -> 13 to "6 phút chạy/đi bộ siêu chậm hạ nhiệt + 7 phút căng cơ sâu."
        "RECOVERY" -> 5 to "3 phút đi bộ chậm + 2 phút dãn cơ nhẹ."
        "CT" -> 5 to "5 phút dãn cơ mông, lưng và tay vai sau tập bổ trợ."
        else -> 0 to "Không cần hồi phục sau tập."
    }
    
    val mainTime = if (workout.targetDistanceKm > 0 && workout.targetPaceSec > 0) {
        ((workout.targetDistanceKm * workout.targetPaceSec) / 60).toInt()
    } else if (workout.type == "CT") {
        35
    } else {
        0
    }
    
    val totalTime = warmupTime + mainTime + cooldownTime
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(36.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when(workout.type) {
                        "EASY" -> "Chạy Thả Lỏng (Easy Run)"
                        "LONG" -> "Chạy Dài Tích Lũy (Long Run)"
                        "TEMPO" -> "Chạy Tốc Độ (Tempo Run)"
                        "RECOVERY" -> "Chạy Phục Hồi (Recovery Run)"
                        "CT" -> "Tập Bổ Trợ (Cross Training)"
                        "RACE" -> "Ngày Đua (Race Day)"
                        else -> "Nghỉ Ngơi (Rest Day)"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when(workout.type) {
                        "EASY" -> "Xây dựng nền tảng tim mạch và độ bền cơ xương khớp ở cường độ thấp."
                        "LONG" -> "Phát triển sức bền tối đa, rèn luyện cơ thể sử dụng mỡ làm năng lượng."
                        "TEMPO" -> "Nâng cao ngưỡng chịu đựng axit lactic (lactate threshold), giúp duy trì tốc độ cao lâu hơn."
                        "RECOVERY" -> "Kích thích tuần hoàn máu mang dinh dưỡng đến cơ bắp để tái tạo và hồi phục."
                        "CT" -> "Duy trì sức bền tim mạch mà không gây va chấn liên tục cho khớp."
                        "RACE" -> "Ngày gặt hái thành quả tập luyện. Tự tin bứt phá!"
                        else -> "Nghỉ ngơi là bắt buộc để cơ thể hồi phục và tự bù đắp khỏe mạnh hơn."
                    },
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                if (totalTime > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Total time badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tổng thời gian: ~ $totalTime phút", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phase 1: Warmup
                    WorkoutPhaseItem(
                        title = "1. Khởi động (Warm-up)",
                        duration = "$warmupTime phút",
                        description = warmupText,
                        color = ColorLong
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Phase 2: Main workout
                    val mainWorkoutDesc = when (workout.type) {
                        "CT" -> "Bơi lội, đạp xe hoặc tập nhóm cơ trung tâm core/lưng trong 35 phút."
                        else -> "Chạy quãng đường ${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km ở pace ${VdotCalculator.formatPace(workout.targetPaceSec)}. Cố gắng giữ nhịp thở đều."
                    }
                    WorkoutPhaseItem(
                        title = "2. Bài tập chính (Main Workout)",
                        duration = "$mainTime phút",
                        description = mainWorkoutDesc,
                        color = color
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Phase 3: Cooldown
                    WorkoutPhaseItem(
                        title = "3. Thả lỏng & Dãn cơ (Cool-down)",
                        duration = "$cooldownTime phút",
                        description = cooldownText,
                        color = ColorEasy
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đã hiểu", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WorkoutPhaseItem(
    title: String,
    duration: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = duration,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}

class DragAndDropState(
    val onDrop: (WorkoutEntity, WorkoutEntity) -> Unit
) {
    var isDragging by mutableStateOf(false)
    var dragItem by mutableStateOf<WorkoutEntity?>(null)
    var hoverItem by mutableStateOf<WorkoutEntity?>(null)
    var initialDragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var currentDragPosition by mutableStateOf(Offset.Zero)
    var localTouchOffset by mutableStateOf(Offset.Zero)

    val itemBounds = mutableMapOf<String, Rect>()

    fun clear() {
        isDragging = false
        dragItem = null
        hoverItem = null
        initialDragPosition = Offset.Zero
        dragOffset = Offset.Zero
        currentDragPosition = Offset.Zero
        localTouchOffset = Offset.Zero
    }
}

@Composable
fun Modifier.workoutDragAndDropTarget(
    workout: WorkoutEntity,
    state: DragAndDropState,
    workoutsList: List<WorkoutEntity>
): Modifier {
    return this
        .onGloballyPositioned { coords ->
            state.itemBounds[workout.date] = coords.boundsInRoot()
        }
        .pointerInput(workout, workoutsList) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val rect = state.itemBounds[workout.date]
                    if (rect != null) {
                        state.dragItem = workout
                        state.isDragging = true
                        state.initialDragPosition = Offset(rect.left + offset.x, rect.top + offset.y)
                        state.currentDragPosition = state.initialDragPosition
                        state.dragOffset = Offset.Zero
                        state.localTouchOffset = offset
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.dragOffset += dragAmount
                    state.currentDragPosition = state.initialDragPosition + state.dragOffset

                    var foundHover: WorkoutEntity? = null
                    val currentPos = state.currentDragPosition
                    for ((date, rect) in state.itemBounds) {
                        if (rect.contains(currentPos)) {
                            val item = workoutsList.find { it.date == date }
                            if (item != null) {
                                foundHover = item
                                break
                            }
                        }
                    }
                    if (foundHover != null && foundHover.weekNumber == state.dragItem?.weekNumber && foundHover.date != state.dragItem?.date) {
                        state.hoverItem = foundHover
                    } else {
                        state.hoverItem = null
                    }
                },
                onDragEnd = {
                    val drag = state.dragItem
                    val hover = state.hoverItem
                    if (drag != null && hover != null && drag.weekNumber == hover.weekNumber) {
                        state.onDrop(drag, hover)
                    }
                    state.clear()
                },
                onDragCancel = {
                    state.clear()
                }
            )
        }
}

@Composable
fun CustomSwapConfirmationDialog(
    sourceWorkout: WorkoutEntity,
    targetWorkout: WorkoutEntity,
    onConfirmAllWeeks: () -> Unit,
    onConfirmThisWeekOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateAStr = remember(sourceWorkout.date) {
        try {
            val d = LocalDate.parse(sourceWorkout.date)
            val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
            "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
        } catch (e: Exception) { sourceWorkout.date }
    }

    val dateBStr = remember(targetWorkout.date) {
        try {
            val d = LocalDate.parse(targetWorkout.date)
            val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
            "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
        } catch (e: Exception) { targetWorkout.date }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Swap Icon and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Tráo Đổi Lịch Tập",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visual representation of the swap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Workout A
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, workoutTypeColor(sourceWorkout).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateAStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sourceWorkout.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (sourceWorkout.type != "REST") {
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", sourceWorkout.targetDistanceKm)} km",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Nghỉ ngơi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "swap",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )

                    // Workout B
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, workoutTypeColor(targetWorkout).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateBStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = targetWorkout.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (targetWorkout.type != "REST") {
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", targetWorkout.targetDistanceKm)} km",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Nghỉ ngơi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bạn muốn tráo đổi bài tập giữa 2 ngày này cho chỉ tuần này hay áp dụng cho toàn bộ các tuần tiếp theo?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions buttons side-by-side with reasonable spacing and icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfirmThisWeekOnly,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chỉ tuần này",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onConfirmAllWeeks,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Các tuần sau",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hủy bỏ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PlanShareDialog(
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onImportCsv: (() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Chia sẻ & Sao lưu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ShareOptionRow(
                    icon = Icons.Default.TableChart,
                    iconColor = Color(0xFF2E7D32),
                    title = "Xuất file CSV",
                    description = "Lưu giáo án dạng bảng tính Excel, Zalo, Drive...",
                    onClick = {
                        onDismiss()
                        onExportCsv()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionRow(
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFFC62828),
                    title = "Xuất file PDF",
                    description = "In ấn hoặc xem giáo án dạng văn bản đẹp mắt",
                    onClick = {
                        onDismiss()
                        onExportPdf()
                    }
                )

                if (onImportCsv != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    ShareOptionRow(
                        icon = Icons.Default.FileUpload,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Nhập từ file CSV",
                        description = "Khôi phục giáo án chạy bộ từ file đã lưu",
                        onClick = {
                            onDismiss()
                            onImportCsv()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShareOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


