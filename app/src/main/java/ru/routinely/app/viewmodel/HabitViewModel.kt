package ru.routinely.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit
import java.util.Calendar
import kotlin.math.max
import androidx.lifecycle.MutableLiveData

// Вспомогательные классы

/**
 * Вспомогательный класс для событий, которые должны быть обработаны только один раз,
 * например, для показа Snackbar или планирования уведомлений.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun observe(owner: androidx.lifecycle.LifecycleOwner, observer: androidx.lifecycle.Observer<in T>) {
        super.observe(owner, { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }
}


/**
 * Запечатанный класс для представления событий, связанных с уведомлениями.
 * Используется для передачи команд из ViewModel в UI-слой.
 */
sealed class NotificationEvent {
    data class Schedule(val habit: Habit) : NotificationEvent()
    data class Cancel(val habitId: Int) : NotificationEvent()
}


// Основной класс ViewModel

/**
 * ViewModel для управления данными привычек.
 * Отвечает за:
 * - Предоставление UI-слою наблюдаемого состояния (список привычек, статистика).
 * - Обработку действий пользователя (создание, удаление, отметка привычки).
 * - Инкапсуляцию бизнес-логики (расчет стриков, управление уведомлениями).
 */
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    // State (Состояния для UI)

    /**
     * Основное состояние экрана "Сегодня" - список привычек на сегодня.
     */
    val habitsForToday: LiveData<List<Habit>> = repository.habitsForToday.asLiveData()

    /**
     * Состояния для экрана "Статистика".
     */
    val totalHabitsCount: LiveData<Int> = repository.totalHabitsCount.asLiveData()
    val bestStreakOverall: LiveData<Int?> = repository.bestStreakOverall.asLiveData()
    val completionDates: LiveData<List<Long>> = repository.completionDates.asLiveData()


    // Events (События для UI)

    private val _notificationEvent = SingleLiveEvent<NotificationEvent>()
    val notificationEvent: LiveData<NotificationEvent> = _notificationEvent


    /**
     * Сохраняет привычку (создает новую или обновляет существующую).
     * Также управляет планированием уведомлений.
     */
    fun saveHabit(habit: Habit) = viewModelScope.launch {
        repository.insert(habit)
        // Если есть время уведомления, отправляем событие на планирование
        if (habit.notificationTime != null) {
            _notificationEvent.value = NotificationEvent.Schedule(habit)
        } else {
            // Если времени нет (или его убрали), отменяем предыдущее
            _notificationEvent.value = NotificationEvent.Cancel(habit.id)
        }
    }

    /**
     * Удаляет привычку и отменяет связанное с ней уведомление.
     */
    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        repository.delete(habit)
        _notificationEvent.value = NotificationEvent.Cancel(habit.id)
    }

    /**
     * Обрабатывает событие отметки/снятия отметки выполнения привычки.
     */
    fun onHabitCheckedChanged(habit: Habit, isCompleted: Boolean) =
        viewModelScope.launch {
            val updatedHabit = calculateNewStreakState(habit, isCompleted)
            repository.update(updatedHabit)
        }

    /**
     * Обновляет прогресс для количественных привычек (например, страницы, минуты).
     * Если цель достигнута, автоматически отмечает привычку как выполненную.
     */
    fun updateHabitProgress(habit: Habit, newProgress: Int) = viewModelScope.launch {
        // Ограничиваем прогресс, чтобы он не превышал целевое значение
        val clampedProgress = newProgress.coerceIn(0, habit.targetValue)

        val updatedHabit = habit.copy(currentValue = clampedProgress)
        repository.update(updatedHabit)

        // Если цель достигнута, а привычка еще не была отмечена сегодня, отмечаем ее
        if (clampedProgress >= habit.targetValue && !isCompletedToday(habit)) {
            onHabitCheckedChanged(updatedHabit, true)
        }
    }

    /**
     * Вызывает удаление всех данных о привычках.
     * Используется для функции "Сброс данных" в настройках.
     */
    fun clearAllData() = viewModelScope.launch {
        repository.clearAllHabits()
    }


    // Private Business Logic

    /**
     * Инкапсулирует логику расчета стриков при изменении статуса выполнения.
     * @return Обновленный объект Habit с корректными значениями стриков.
     */
    private fun calculateNewStreakState(habit: Habit, isCompleted: Boolean): Habit {
        if (isCompleted) {
            // Пользователь отметил выполнение привычки.
            val today = System.currentTimeMillis()
            val wasCompletedYesterday = habit.lastCompletedDate?.let {
                isYesterday(it)
            } ?: false

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
                bestStreak = newBestStreak,
                currentValue = habit.targetValue // При отметке чекбокса всегда считаем цель достигнутой
            )

        } else {
            // Пользователь снял отметку о выполнении.
            val wasCompletedToday = isCompletedToday(habit)

            return habit.copy(
                lastCompletedDate = null, // Сбрасываем дату сегодняшнего выполнения
                // Уменьшаем стрик, только если он был увеличен СЕГОДНЯ
                currentStreak = if (wasCompletedToday) habit.currentStreak - 1 else habit.currentStreak,
                currentValue = 0 // Сбрасываем прогресс
            )
        }
    }

    private fun isCompletedToday(habit: Habit): Boolean {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return habit.lastCompletedDate != null && habit.lastCompletedDate!! >= todayStart
    }

    /**
     * Вспомогательная функция для проверки, является ли переданная
     * временная метка "вчерашним днем".
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