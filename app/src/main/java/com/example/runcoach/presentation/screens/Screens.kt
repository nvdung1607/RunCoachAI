package com.example.runcoach.presentation.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
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
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
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
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
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
                        viewModel.saveOnboarding(raceDateText, fitnessLevel, targetDistance, maxSessions)
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
        }
    }
}

// ─────────────────────────────────────────────
// TEST RUN SCREEN
// ─────────────────────────────────────────────
@Composable
fun TestRunScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit
) {
    var minText by remember { mutableStateOf("") }
    var secText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

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
                    Toast.makeText(context, "Đã tự lấy kết quả chạy tốt nhất từ Health Connect!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Không tìm thấy bài chạy 3km nào gần đây.", Toast.LENGTH_LONG).show()
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
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
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
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Thời gian chạy 3km tốt nhất", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Phút") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secText,
                            onValueChange = { secText = it },
                            label = { Text("Giây") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
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
                                        fontSize = 12.sp,
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
                        Toast.makeText(context, "Nhập thời gian hợp lệ cho 3km (6–40 phút)!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    viewModel.completeTestRun(totalSec)
                    onNavigateToDashboard()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Sinh Giáo Án & Bắt Đầu →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
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
            .statusBarsPadding()
            .navigationBarsPadding(),
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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

        // Health Connect card
        item {
            HealthConnectCard(
                hcStatus = hcStatus,
                hcPermissionsGranted = hcPermissionsGranted,
                onRequestPermissions = { permissionsLauncher.launch(healthConnectManager.requiredPermissions) },
                onSync = {
                    viewModel.triggerSync()
                    Toast.makeText(context, "Bắt đầu đồng bộ từ Health Connect...", Toast.LENGTH_SHORT).show()
                }
            )
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
                                "🎯 Mục tiêu: ${workout.targetDistanceKm}km @ ${VdotCalculator.formatPace(workout.targetPaceSec)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(workout.instructions, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                        if (workout.isCompleted) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                "✅ Thực tế: ${workout.actualDistanceKm}km trong ${workout.actualDurationMin.toInt()} phút (${workout.syncSource})",
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
                                        viewModel.triggerSync()
                                        Toast.makeText(context, "Đang đồng bộ từ Health Connect...", Toast.LENGTH_SHORT).show()
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

        // Weekly Volume Chart
        item {
            WeeklyVolumeChart(workoutsList = workoutsList)
        }

        // Navigation buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToPlan,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(50.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Giáo Án", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
                
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(50.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lịch Sử", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onNavigateToCalendar,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(50.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lịch Tháng", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
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
                                text = "Mục tiêu: ${workout.targetDistanceKm} km @ ${VdotCalculator.formatPace(workout.targetPaceSec)} min/km",
                                fontSize = 12.5.sp,
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Exactly 3 Helper Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Button 1: Đúng mục tiêu
                        val targetDurationMin = ((workout.targetDistanceKm * workout.targetPaceSec) / 60.0).toInt()
                        Button(
                            onClick = {
                                logDistanceText = workout.targetDistanceKm.toString()
                                logDurationText = targetDurationMin.toString()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đúng mục tiêu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Button 2: +0.5 km
                        OutlinedButton(
                            onClick = {
                                val current = logDistanceText.toDoubleOrNull() ?: 0.0
                                logDistanceText = String.format(java.util.Locale.US, "%.1f", current + 0.5)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("+0.5 km", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Button 3: +5 phút
                        OutlinedButton(
                            onClick = {
                                val current = logDurationText.toIntOrNull() ?: 0
                                logDurationText = (current + 5).toString()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("+5 phút", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                        text = "Pace trung bình thực tế: $paceStr /km",
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Khối lượng tập luyện theo tuần", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))

            val weeklyStats = remember(workoutsList) {
                workoutsList.groupBy { it.weekNumber }.map { (week, list) ->
                    val target = list.sumOf { it.targetDistanceKm }
                    val actual = list.sumOf { it.actualDistanceKm }
                    week to Pair(target, actual)
                }.sortedBy { it.first }
            }

            if (weeklyStats.isNotEmpty()) {
                val maxVolume = weeklyStats.maxOfOrNull { maxOf(it.second.first, it.second.second) }?.coerceAtLeast(10.0) ?: 10.0

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
            Toast.makeText(context, "⚠️ Cần cấp quyền thông báo trong cài đặt để nhận nhắc nhở chạy bộ.", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(context, "⚠️ Vui lòng cấp quyền thông báo để lưu cài đặt này.", Toast.LENGTH_LONG).show()
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
    onNavigateBack: () -> Unit
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        // Week header
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
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                        1 -> Toast.makeText(context, "⚠️ Đã dời sang $newDate (ngày đó đã có: $conflictDesc)", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(context, "❌ Không tìm thấy bài tập gốc.", Toast.LENGTH_SHORT).show()
                    }
                }
                showRescheduleDialog = false
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
fun WorkoutDayRow(workout: WorkoutEntity, onReschedule: () -> Unit, onClick: () -> Unit) {
    val wColor = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    workout.isSkipped -> ColorSkipped.copy(alpha = 0.05f)
                    workout.isCompleted -> ColorCompleted.copy(alpha = 0.05f)
                    else -> MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                }
            )
            .border(1.dp, wColor.copy(alpha = if (workout.isSkipped) 0.1f else 0.15f), RoundedCornerShape(12.dp))
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
                Text(dayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("${workout.actualDistanceKm}km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorCompleted)
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorCompleted, modifier = Modifier.size(16.dp))
                }
            }
            workout.targetDistanceKm > 0 -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${workout.targetDistanceKm}km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                        else -> "Chạy quãng đường ${workout.targetDistanceKm} km ở pace ${VdotCalculator.formatPace(workout.targetPaceSec)}. Cố gắng giữ nhịp thở đều."
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
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
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
