package ru.routinely.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.routinely.app.model.Habit

/**
 * DAO (Data Access Object) для сущности Habit.
 * Предоставляет методы для всех операций с таблицей `habits` в базе данных.
 */
@Dao
interface HabitDao {

    // --- CRUD-операции (Create, Read, Update, Delete) ---

    /**
     * Вставляет новую привычку в базу данных.
     * В случае конфликта (например, одинаковый id) заменяет старую запись.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    /**
     * Обновляет существующую привычку.
     * Поиск осуществляется по Primary Key объекта.
     */
    @Update
    suspend fun updateHabit(habit: Habit)

    /**
     * Удаляет привычку из базы данных.
     */
    @Delete
    suspend fun deleteHabit(habit: Habit)

    /**
     * Возвращает Flow со списком всех привычек, отсортированных по дате создания.
     */
    @Query("SELECT * FROM habits ORDER BY creation_date DESC")
    fun getAllHabits(): Flow<List<Habit>>

    /**
     * Получает одну привычку по ее ID.
     */
    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitById(habitId: Int): Flow<Habit?>


    // --- Специфичные запросы для обновления и статистики ---

    /**
     * Обновляет поля, связанные со стриком, для конкретной привычки.
     * Более производительно, чем загружать и сохранять весь объект.
     * @param habitId ID обновляемой привычки.
     * @param lastCompletedDate Новая дата последнего выполнения.
     * @param currentStreak Новое значение текущей серии.
     * @param bestStreak Новое значение лучшей серии.
     */
    @Query("UPDATE habits SET last_completed_date = :lastCompletedDate, current_streak = :currentStreak, best_streak = :bestStreak WHERE id = :habitId")
    suspend fun updateStreak(habitId: Int, lastCompletedDate: Long?, currentStreak: Int, bestStreak: Int)

    /**
     * Обновляет текущее значение выполнения для привычек с прогрессом (например, страницы, километры).
     * @param habitId ID обновляемой привычки.
     * @param newValue Новое значение прогресса.
     */
    @Query("UPDATE habits SET current_value = :newValue WHERE id = :habitId")
    suspend fun updateCurrentValue(habitId: Int, newValue: Int)

    /**
     * Возвращает лучшую серию (стрик) среди всех привычек.
     */
    @Query("SELECT MAX(best_streak) FROM habits")
    fun getBestStreakOverall(): Flow<Int?>

    /**
     * Возвращает общее количество созданных привычек.
     */
    @Query("SELECT COUNT(id) FROM habits")
    fun getTotalHabitsCount(): Flow<Int>

    /**
     * Очищает всю таблицу с привычками.
     * Используется для функции "Сброс данных" в настройках.
     */
    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()
}