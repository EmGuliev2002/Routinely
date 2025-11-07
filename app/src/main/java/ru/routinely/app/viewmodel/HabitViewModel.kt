package ru.routinely.app.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit
import java.util.Calendar
import kotlin.math.max

/**
 * ViewModel для главного экрана со списком привычек.
 *
 * Отвечает за:
 * - Предоставление UI-слою наблюдаемого состояния (список привычек).
 * - Обработку действий пользователя (создание, удаление, отметка привычки).
 * - Инкапсуляцию бизнес-логики (например, расчет стриков).
 */
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    // --- State ---

    /**
     * Основное состояние экрана - список всех привычек.
     */

    val habitsForToday: LiveData<List<Habit>> = repository.habitsForToday.asLiveData()
    // --- Events (Public API for the UI) ---

    /**
     * Сохраняет привычку (создает новую или обновляет существующую).
     */
    fun saveHabit(habit: Habit) = viewModelScope.launch {
        repository.insert(habit) // insert c onConflict = REPLACE работает и для создания, и для обновления
    }

    /**
     * Удаляет привычку.
     */
    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        repository.delete(habit)
    }

    /**
     * Обрабатывает событие отметки/снятия отметки выполнения привычки.
     * @param habit Привычка, статус которой изменился.
     * @param isCompleted Новое состояние чекбокса.
     */
    fun onHabitCheckedChanged(habit: Habit, isCompleted: Boolean) = viewModelScope.launch {
        // Рассчитываем новое состояние привычки, включая обновленные стрики.
        val updatedHabit = calculateNewStreakState(habit, isCompleted)

        // Сохраняем обновленную привычку в базу данных.
        repository.update(updatedHabit)
    }


    // --- Private Business Logic ---

    /**
     * Инкапсулирует логику расчета стриков при изменении статуса выполнения.
     * @return Обновленный объект Habit с корректными значениями стриков.
     */
    private fun calculateNewStreakState(habit: Habit, isCompleted: Boolean): Habit {
        if (isCompleted) {
            // Пользователь отметил выполнение привычки.
            val today = System.currentTimeMillis()
            val wasCompletedYesterday = habit.lastCompletedDate?.let { isYesterday(it) } ?: false

            val newCurrentStreak = if (wasCompletedYesterday) {
                // Если последнее выполнение было вчера, продолжаем стрик.
                habit.currentStreak + 1
            } else {
                // Если стрик прерван, начинаем новый.
                1
            }

            // Обновляем лучший стрик, если текущий его превзошел.
            val newBestStreak = max(newCurrentStreak, habit.bestStreak)

            return habit.copy(
                lastCompletedDate = today,
                currentStreak = newCurrentStreak,
                bestStreak = newBestStreak
            )
        } else {
            // Пользователь снял отметку о выполнении.
            return habit.copy(
                lastCompletedDate = null, // Сбрасываем дату сегодняшнего выполнения
                currentStreak = habit.currentStreak - 1 // Уменьшаем стрик, если он был увеличен сегодня
            )
        }
    }

    /**
     * Вспомогательная функция для проверки, является ли переданная временная метка "вчерашним днем".
     * Корректно обрабатывает переходы через месяц и год.
     */
    private fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val completionDate = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return yesterday.get(Calendar.YEAR) == completionDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == completionDate.get(Calendar.DAY_OF_YEAR)
    }
}