package ru.routinely.app.data

import kotlinx.coroutines.flow.Flow
import ru.routinely.app.model.Habit

/**
 * Репозиторий для управления сущностями Habit.
 * Абстрагирует ViewModel от деталей реализации источников данных (в данном случае, Room DAO).
 * Весь доступ к данным должен осуществляться через этот класс.
 */
class HabitRepository(private val habitDao: HabitDao) {

    /**
     * Предоставляет наблюдаемый Flow со списком всех привычек.
     * ViewModel подписывается на этот поток для получения обновлений в реальном времени.
     */
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()


    // --- Методы для модификации данных ---

    /**
     * Вставляет или обновляет привычку в источнике данных.
     * @param habit Объект привычки для сохранения.
     */
    suspend fun insert(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    /**
     * Обновляет существующую привычку.
     * @param habit Объект привычки для обновления.
     */
    suspend fun update(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    /**
     * Удаляет привычку из источника данных.
     * @param habit Объект привычки для удаления.
     */
    suspend fun delete(habit: Habit) {
        habitDao.deleteHabit(habit)
    }


    // --- Методы для получения статистических данных ---

    /**
     * Предоставляет Flow с общим количеством созданных привычек.
     */
    val totalHabitsCount: Flow<Int> = habitDao.getTotalHabitsCount()

    /**
     * Предоставляет Flow с лучшей серией выполнения среди всех привычек.
     */
    val bestStreakOverall: Flow<Int?> = habitDao.getBestStreakOverall()


    // --- Методы для специфичных операций ---

    /**
     * Очищает все данные о привычках.
     * Используется для функции сброса данных в настройках.
     */
    suspend fun clearAllHabits() {
        habitDao.clearAllHabits()
    }
}