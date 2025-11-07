package ru.routinely.app.data

import kotlinx.coroutines.flow.Flow
import ru.routinely.app.model.Habit
import kotlinx.coroutines.flow.map // Обязательный импорт для фильтрации
import java.util.Calendar // Обязательный импорт для определения дня недели

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


    // --- НОВЫЙ РЕАЛИЗОВАННЫЙ Flow для UI ---
    /**
     * Предоставляет Flow со списком привычек, которые должны быть выполнены СЕГОДНЯ.
     * Использует логику расписания (поле type) для фильтрации.
     */
    val habitsForToday: Flow<List<Habit>> = allHabits.map { habits ->
        // Определяем текущий день недели (1=Понедельник, 7=Воскресенье)
        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { dayOfWeek ->
            // Calendar: 1=Вс, 2=Пн, 3=Вт... Нам нужен формат 1=Пн, 7=Вс
            if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        }

        // Фильтруем список
        habits.filter { habit ->
            val type = habit.type

            when {
                // 1. Ежедневная привычка
                type == "daily" -> true

                // 2. Привычка по дням недели (type = "1,3,5,6,7")
                type.contains(',') -> {
                    // Преобразуем строку "1,3,5" в набор чисел {1, 3, 5}
                    val selectedDays = type.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                    todayIndex in selectedDays
                }

                // 3. Другие/неизвестные типы
                else -> false
            }
        }
    }


    // --- Методы для модификации данных ---

    // ... (остальной код insert, update, delete, totalHabitsCount, bestStreakOverall) ...
    // Вставь новый Flow (habitsForToday) сразу после val allHabits,
    // а остальные методы оставь как есть.


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
        // Убедитесь, что этот метод есть в HabitDao
        // habitDao.clearAllHabits()
    }
}