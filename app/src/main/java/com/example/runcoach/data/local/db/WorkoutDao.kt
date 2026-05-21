package com.example.runcoach.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE date = :date LIMIT 1")
    suspend fun getWorkoutByDate(date: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE date = :date LIMIT 1")
    fun getWorkoutFlowByDate(date: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts ORDER BY date ASC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE weekNumber = :weekNumber ORDER BY date ASC")
    fun getWorkoutsByWeekFlow(weekNumber: Int): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    suspend fun getWorkoutsByMonth(monthPrefix: String): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getWorkoutsByDateRange(startDate: String, endDate: String): List<WorkoutEntity>

    @Query("SELECT COUNT(*) FROM workouts WHERE weekNumber = :weekNumber AND type IN ('EASY','LONG','TEMPO','RECOVERY') AND isCompleted = 1")
    suspend fun countCompletedRunsInWeek(weekNumber: Int): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE weekNumber = :weekNumber AND type IN ('EASY','LONG','TEMPO','RECOVERY')")
    suspend fun countRunsInWeek(weekNumber: Int): Int

    @Query("DELETE FROM workouts")
    suspend fun clearAllWorkouts()
}
