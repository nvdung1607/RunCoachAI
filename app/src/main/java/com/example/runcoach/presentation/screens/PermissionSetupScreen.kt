package com.example.runcoach.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.runcoach.R
import com.example.runcoach.data.health.HealthConnectManager
import com.example.runcoach.presentation.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    viewModel: MainViewModel,
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPrefs by viewModel.userPreferences.collectAsState()
    
    // Theme evaluation
    val isDarkTheme = when (userPrefs.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    
    // Notification Permission State
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Đã cấp quyền thông báo thành công", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Quyền thông báo bị từ chối. Bạn có thể bật lại sau.", Toast.LENGTH_LONG).show()
        }
    }

    // Health Connect Permission State
    val healthConnectManager = remember { HealthConnectManager(context) }
    var hcPermissionsGranted by remember { mutableStateOf(false) }
    var isHcAvailable by remember { mutableStateOf(false) }
    var isHcNotInstalled by remember { mutableStateOf(false) }

    val hcStatus = remember {
        try {
            HealthConnectClient.getSdkStatus(context)
        } catch (e: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    LaunchedEffect(Unit) {
        isHcAvailable = hcStatus == HealthConnectClient.SDK_AVAILABLE
        isHcNotInstalled = hcStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        hcPermissionsGranted = healthConnectManager.hasPermissions()
    }

    val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        android.util.Log.d("RunCoachHC", "==== HEALTH CONNECT PERMISSION LAUNCHER RESULT ====")
        android.util.Log.d("RunCoachHC", "Required Permissions: ${healthConnectManager.requiredPermissions}")
        android.util.Log.d("RunCoachHC", "Granted Permissions Returned: $granted")
        
        val missingPermissions = healthConnectManager.requiredPermissions.filter { it !in granted }
        if (missingPermissions.isNotEmpty()) {
            android.util.Log.w("RunCoachHC", "Missing Permissions: $missingPermissions")
        } else {
            android.util.Log.i("RunCoachHC", "All permissions successfully granted!")
        }

        hcPermissionsGranted = granted.containsAll(healthConnectManager.requiredPermissions)
        android.util.Log.d("RunCoachHC", "hcPermissionsGranted flag set to: $hcPermissionsGranted")
        android.util.Log.d("RunCoachHC", "==================================================")

        if (hcPermissionsGranted) {
            Toast.makeText(context, "Đã kết nối Health Connect thành công!", Toast.LENGTH_SHORT).show()
        } else {
            val details = if (missingPermissions.isNotEmpty()) {
                "\nThiếu quyền: ${missingPermissions.map { it.substringAfterLast(".") }.joinToString(", ")}"
            } else ""
            Toast.makeText(context, "Chưa cấp đủ quyền kết nối Health Connect.$details", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF0F0F10), Color(0xFF1E1B2C))
                    } else {
                        listOf(Color(0xFFF9FAFB), Color(0xFFEFF6FF))
                    }
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Intro Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // App Logo
                Image(
                    painter = painterResource(
                        id = if (isDarkTheme) R.drawable.app_logo_dark else R.drawable.app_logo_light
                    ),
                    contentDescription = "RunCoach Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Quyền và kết nối",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Để RunCoach AI đồng bộ chỉ số chạy bộ và nhắc nhở lịch tập chính xác, hãy cấp các quyền truy cập cơ bản dưới đây.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Body Section: Permission Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Notification Permission
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            Color(0xFF1E1E24)
                        } else {
                            Color.White
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hasNotificationPermission) {
                                        Color(0xFF10B981).copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Notifications,
                                contentDescription = "Notification Icon",
                                tint = if (hasNotificationPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thông báo nhắc lịch tập",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nhắc nhở bạn chạy bộ đúng ngày, đúng giờ và thông báo lời khuyên từ huấn luyện viên.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (hasNotificationPermission) {
                                Text(
                                    text = "✅ Đã cấp quyền thông báo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            } else {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            hasNotificationPermission = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("Cấp quyền thông báo", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // Card 2: Health Connect Connection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            Color(0xFF1E1E24)
                        } else {
                            Color.White
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hcPermissionsGranted) {
                                        Color(0xFF10B981).copy(alpha = 0.15f)
                                    } else {
                                        Color(0xFFD946EF).copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hcPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Favorite,
                                contentDescription = "Health Connect Icon",
                                tint = if (hcPermissionsGranted) Color(0xFF10B981) else Color(0xFFD946EF),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đồng bộ Health Connect",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nhập dữ liệu tự động từ các app đồng hồ thể thao (Garmin, Strava, Nike Run Club, v.v.).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            when {
                                hcPermissionsGranted -> {
                                    Text(
                                        text = "✅ Đã đồng bộ thành công",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                isHcAvailable -> {
                                    Button(
                                        onClick = {
                                            android.util.Log.d("RunCoachHC", "User clicked 'Kết nối thiết bị'. Launching permission request...")
                                            android.util.Log.d("RunCoachHC", "Requesting permissions list: ${healthConnectManager.requiredPermissions}")
                                            healthConnectPermissionsLauncher.launch(healthConnectManager.requiredPermissions)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD946EF)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Kết nối thiết bị", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                                isHcNotInstalled || hcStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                                    Column {
                                        Text(
                                            text = "⚠️ Thiết bị chưa cài Health Connect",
                                            fontSize = 12.sp,
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                                    }
                                                    context.startActivity(webIntent)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3B82F6)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text("Tải từ Google Play", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                                else -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Not supported",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Không khả dụng trên thiết bị này",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Section: Navigation & Hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.completePermissionSetup()
                            onNavigateNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                    )
                ) {
                    Text(
                        text = "Tiếp tục",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bạn luôn có thể quản lý các quyền này trong mục Cài đặt ứng dụng.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
