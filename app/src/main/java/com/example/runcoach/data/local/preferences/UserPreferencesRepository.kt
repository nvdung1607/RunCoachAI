package com.example.runcoach.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val raceDate: String,
    val fitnessLevel: String,
    val vdotScore: Float,
    val hasCompletedTestRun: Boolean,
    val easyPaceSec: Int,
    val tempoPaceSec: Int,
    val longPaceSec: Int,
    val themeMode: String,             // "LIGHT", "DARK", "SYSTEM"
    val isNotificationEnabled: Boolean,
    val notificationHour: Int,         // 0-23, default 6
    val notificationMinute: Int,       // 0-59, default 0
    val targetDistance: Int,           // 5, 10, 21, 42 km
    val maxSessionsPerWeek: Int,       // 2, 3, 4, 5 days per week
    val hasCompletedPermissionSetup: Boolean, // true when permission onboarding screen is completed
    val gender: String = "MALE",       // "MALE", "FEMALE", "OTHER"
    val age: Int = 25,                 // user age
    val activityLevel: String = "SEDENTARY", // "SEDENTARY", "ACTIVE", "RUNNER"
    val isLoaded: Boolean = false       // true when preferences are successfully loaded from disk
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val RACE_DATE = stringPreferencesKey("race_date")
        val FITNESS_LEVEL = stringPreferencesKey("fitness_level")
        val VDOT_SCORE = floatPreferencesKey("vdot_score")
        val HAS_COMPLETED_TEST_RUN = booleanPreferencesKey("has_completed_test_run")
        val EASY_PACE_SEC = intPreferencesKey("easy_pace_sec")
        val TEMPO_PACE_SEC = intPreferencesKey("tempo_pace_sec")
        val LONG_PACE_SEC = intPreferencesKey("long_pace_sec")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_NOTIFICATION_ENABLED = booleanPreferencesKey("is_notification_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val TARGET_DISTANCE = intPreferencesKey("target_distance")
        val MAX_SESSIONS_PER_WEEK = intPreferencesKey("max_sessions_per_week")
        val HAS_COMPLETED_PERMISSION_SETUP = booleanPreferencesKey("has_completed_permission_setup")
        val GENDER = stringPreferencesKey("gender")
        val AGE = intPreferencesKey("age")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                raceDate = preferences[PreferencesKeys.RACE_DATE] ?: "",
                fitnessLevel = preferences[PreferencesKeys.FITNESS_LEVEL] ?: "BEGINNER",
                vdotScore = preferences[PreferencesKeys.VDOT_SCORE] ?: 0f,
                hasCompletedTestRun = preferences[PreferencesKeys.HAS_COMPLETED_TEST_RUN] ?: false,
                easyPaceSec = preferences[PreferencesKeys.EASY_PACE_SEC] ?: 480,
                tempoPaceSec = preferences[PreferencesKeys.TEMPO_PACE_SEC] ?: 420,
                longPaceSec = preferences[PreferencesKeys.LONG_PACE_SEC] ?: 510,
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                isNotificationEnabled = preferences[PreferencesKeys.IS_NOTIFICATION_ENABLED] ?: true,
                notificationHour = preferences[PreferencesKeys.NOTIFICATION_HOUR] ?: 6,
                notificationMinute = preferences[PreferencesKeys.NOTIFICATION_MINUTE] ?: 0,
                targetDistance = preferences[PreferencesKeys.TARGET_DISTANCE] ?: 21,
                maxSessionsPerWeek = preferences[PreferencesKeys.MAX_SESSIONS_PER_WEEK] ?: 3,
                hasCompletedPermissionSetup = preferences[PreferencesKeys.HAS_COMPLETED_PERMISSION_SETUP] ?: false,
                gender = preferences[PreferencesKeys.GENDER] ?: "MALE",
                age = preferences[PreferencesKeys.AGE] ?: 25,
                activityLevel = preferences[PreferencesKeys.ACTIVITY_LEVEL] ?: "SEDENTARY",
                isLoaded = true
            )
        }

    suspend fun saveOnboardingPreferences(
        raceDate: String,
        fitnessLevel: String,
        targetDistance: Int,
        maxSessions: Int,
        gender: String,
        age: Int,
        activityLevel: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RACE_DATE] = raceDate
            preferences[PreferencesKeys.FITNESS_LEVEL] = fitnessLevel
            preferences[PreferencesKeys.TARGET_DISTANCE] = targetDistance
            preferences[PreferencesKeys.MAX_SESSIONS_PER_WEEK] = maxSessions
            preferences[PreferencesKeys.GENDER] = gender
            preferences[PreferencesKeys.AGE] = age
            preferences[PreferencesKeys.ACTIVITY_LEVEL] = activityLevel
        }
    }

    suspend fun saveFitnessProfile(vdotScore: Float, easyPaceSec: Int, tempoPaceSec: Int, longPaceSec: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VDOT_SCORE] = vdotScore
            preferences[PreferencesKeys.EASY_PACE_SEC] = easyPaceSec
            preferences[PreferencesKeys.TEMPO_PACE_SEC] = tempoPaceSec
            preferences[PreferencesKeys.LONG_PACE_SEC] = longPaceSec
            preferences[PreferencesKeys.HAS_COMPLETED_TEST_RUN] = true
        }
    }

    suspend fun savePermissionSetupCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_PERMISSION_SETUP] = true
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_HOUR] = hour
            preferences[PreferencesKeys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
