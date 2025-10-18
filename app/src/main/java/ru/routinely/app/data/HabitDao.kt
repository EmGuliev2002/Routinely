package ru.routinely.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.routinely.app.model.Habit

@Dao
interface HabitDao {
    @Insert
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits ORDER BY creationDate DESC")
    fun getAllHabits(): Flow<List<Habit>>
}