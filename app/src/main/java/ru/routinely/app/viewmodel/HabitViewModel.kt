package ru.routinely.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit
import ru.routinely.app.utils.HabitFilter
import ru.routinely.app.utils.SortOrder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

// Вспомогательные классы

/**
 * Вспомогательный класс для событий, которые должны быть
 * обработаны только один раз,
 * например, для показа Snackbar или планирования уведомлений.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    override fun observe(owner: androidx.lifecycle.LifecycleOwner, observer: androidx.lifecycle.Observer<in T>) {
        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }
}

/**
 * Запечатанный класс для представления событий, связанных с
 * уведомлениями.
 * Используется для передачи команд из ViewModel в UI-слой.
 */
sealed class NotificationEvent {
    data class Schedule(val habit: Habit) : NotificationEvent()
    data class Cancel(val habitId: Int) : NotificationEvent()
}

// Модель, представляющая полное состояние UI для главного экрана
data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val categories: List<String> = emptyList(),
    val sortOrder: SortOrder = SortOrder.BY_DATE,
    val habitFilter: HabitFilter = HabitFilter.TODAY,
    val categoryFilter: String? = null,
    val isNameSortAsc: Boolean = true
)

data class CalendarDayState(
    val date: LocalDate,
    val isCompleted: Boolean,
    val isSelected: Boolean
)

data class DayCompletion(
    val date: LocalDate,
    val completionRatio: Float
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val totalHabitsCount: Int = 0,
    val bestStreakOverall: Int = 0,
    val weeklyCompletionPercentage: Int = 0,
    val monthlyCompletionPercentage: Int = 0,
    val calendarDays: List<CalendarDayState> = emptyList(),
    val weekRangeLabel: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateHabits: List<Habit> = emptyList(),
    val weeklyTrend: List<DayCompletion> = emptyList()
)

// Основной класс ViewModel

/**
 * ViewModel для управления данными привычек.
 */
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    // Состояния для UI

    private val _sortOrder = MutableStateFlow(SortOrder.BY_DATE)
    private val _habitFilter = MutableStateFlow(HabitFilter.TODAY)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _isNameSortAsc = MutableStateFlow(true)

    // Внутренний класс для группировки всех настроек пользователя
    private data class UserOptions(
        val sortOrder: SortOrder,
        val habitFilter: HabitFilter,
        val categoryFilter: String?,
        val isNameSortAsc: Boolean
    )

    // 1. Создаем единый поток, который будет реагировать на ЛЮБОЕ изменение настроек
    private val userOptionsFlow = combine(
        _sortOrder, _habitFilter, _categoryFilter, _isNameSortAsc
    ) { sort, filter, category, isAsc ->
        UserOptions(sort, filter, category, isAsc)
    }

    // 2. Основной поток состояния UI
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HabitUiState> = userOptionsFlow.flatMapLatest { options ->
        // flatMapLatest перезапускает этот блок каждый раз, когда меняются `options`.
        // Это позволяет нам выбрать правильный запрос к БД.
        val sortedHabitsFlow = when (options.sortOrder) {
            SortOrder.BY_DATE -> repository.allHabits
            SortOrder.BY_STREAK -> repository.getAllHabitsSortedByStreak()
            SortOrder.BY_NAME -> if (options.isNameSortAsc) repository.getAllHabitsSortedByNameASC() else repository.getAllHabitsSortedByNameDESC()
        }

        // 3. Комбинируем отсортированные данные с потоком категорий
        combine(
            sortedHabitsFlow,
            repository.allHabits.map { it.mapNotNull { habit -> habit.category }.distinct().sorted() }
        ) { habits, allCategories ->
            // 4. Применяем фильтры (это уже происходит в памяти, без запроса к БД)
            val filteredHabits = when (options.habitFilter) {
                HabitFilter.TODAY -> filterForToday(habits)
                HabitFilter.ALL -> habits
                HabitFilter.UNCOMPLETED -> habits.filter { !isCompletedToday(it) }
            }.let { filteredList ->
                if (options.categoryFilter != null) {
                    filteredList.filter { it.category == options.categoryFilter }
                } else {
                    filteredList
                }
            }

            // 5. Формируем итоговое состояние UI
            HabitUiState(
                habits = filteredHabits,
                categories = allCategories,
                sortOrder = options.sortOrder,
                habitFilter = options.habitFilter,
                categoryFilter = options.categoryFilter,
                isNameSortAsc = options.isNameSortAsc
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HabitUiState()
    )


    // Состояния для экрана "Статистика"
    val totalHabitsCount: LiveData<Int> = repository.totalHabitsCount.asLiveData()
    val bestStreakOverall: LiveData<Int?> = repository.bestStreakOverall.asLiveData()
    val completionDates: LiveData<List<Long>> = repository.completionDates.asLiveData()

    private val _selectedStatsDate = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val statsUiState: StateFlow<StatsUiState> = combine(
        repository.totalHabitsCount,
        repository.bestStreakOverall,
        repository.completionDates,
        repository.allHabits,
        _selectedStatsDate
    ) { totalCount, bestStreak, completionDates, habits, selectedDate ->
        val completionDatesSet = completionDates.map { it.toLocalDate() }.toSet()
        val now = LocalDate.now()
        val weeklyPercentage = calculateCompletionPercentage(
            completionDatesSet,
            startDate = now.minusDays(6),
            endDate = now
        )
        val currentMonthStart = now.withDayOfMonth(1)
        val monthlyPercentage = calculateCompletionPercentage(
            completionDatesSet,
            startDate = currentMonthStart,
            endDate = currentMonthStart.withDayOfMonth(YearMonth.from(now).lengthOfMonth())
        )

        val calendarDays = buildWeekCalendar(completionDatesSet, selectedDate)
        val weekRangeLabel = formatWeekRange(selectedDate)
        val habitsByDate = habits
            .mapNotNull { habit ->
                habit.lastCompletedDate?.let { timestamp ->
                    timestamp.toLocalDate() to habit
                }
            }
            .groupBy({ it.first }, { it.second })

        val selectedHabits = habitsByDate[selectedDate].orEmpty()
        val weeklyTrend = buildWeeklyTrend(habitsByDate, totalCount, now)

        StatsUiState(
            isLoading = false,
            totalHabitsCount = totalCount,
            bestStreakOverall = (bestStreak ?: 0),
            weeklyCompletionPercentage = weeklyPercentage,
            monthlyCompletionPercentage = monthlyPercentage,
            calendarDays = calendarDays,
            weekRangeLabel = weekRangeLabel,
            selectedDate = selectedDate,
            selectedDateHabits = selectedHabits,
            weeklyTrend = weeklyTrend
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StatsUiState()
    )

    // События для UI
    private val _notificationEvent = SingleLiveEvent<NotificationEvent>()
    val notificationEvent: LiveData<NotificationEvent> = _notificationEvent


    // Методы для вызова из UI

    fun setSortOrder(sortOrder: SortOrder) {
        if (sortOrder == SortOrder.BY_NAME && _sortOrder.value == SortOrder.BY_NAME) {
            _isNameSortAsc.value = !_isNameSortAsc.value
        }
        _sortOrder.value = sortOrder
    }

    fun setFilter(filter: HabitFilter) {
        _habitFilter.value = filter
        _categoryFilter.value = null
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        if (category != null) {
            _habitFilter.value = HabitFilter.ALL
        }
    }

    // Остальные методы ViewModel
    fun saveHabit(habit: Habit) = viewModelScope.launch {
        repository.insert(habit)
        if (habit.notificationTime != null) {
            _notificationEvent.value = NotificationEvent.Schedule(habit)
        } else {
            _notificationEvent.value = NotificationEvent.Cancel(habit.id)
        }
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        repository.delete(habit)
        _notificationEvent.value = NotificationEvent.Cancel(habit.id)
    }

    fun onHabitCheckedChanged(habit: Habit, isCompleted: Boolean) = viewModelScope.launch {
        val updatedHabit = calculateNewStreakState(habit, isCompleted)
        repository.update(updatedHabit)
    }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllHabits()
    }

    fun onStatsDateSelected(date: LocalDate) {
        _selectedStatsDate.value = date
    }

    // Private Business Logic
    private fun filterForToday(habits: List<Habit>): List<Habit> {
        val todayCalendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayIndex = getRoutinelyDayOfWeek(todayCalendarDay)
        return habits.filter { habit ->
            when {
                habit.type == "daily" -> true
                habit.type.isNotEmpty() && habit.type != "daily" -> {
                    val selectedDays = habit.type.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                    todayIndex in selectedDays
                }
                else -> false
            }
        }
    }

    private fun getRoutinelyDayOfWeek(calendarDay: Int): Int {
        return if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
    }

    private fun calculateNewStreakState(habit: Habit, isCompleted: Boolean): Habit {
        val today = Calendar.getInstance().timeInMillis

        // --- 1. РАСЧЕТ НОВОГО currentValue (+1 или -1) ---
        val newCurrentValue = if (isCompleted) {
            // ИНКРЕМЕНТ: +1, не превышая targetValue
            (habit.currentValue + 1).coerceAtMost(habit.targetValue)
        } else {
            // ДЕКРЕМЕНТ: -1, не опускаясь ниже 0
            (habit.currentValue - 1).coerceAtLeast(0)
        }

        // --- 2. ОПРЕДЕЛЕНИЕ СТАТУСА АКТИВНОСТИ ---
        // Активность есть, если newCurrentValue > 0. Этот флаг теперь управляет стриком.
        val wasActiveToday = newCurrentValue > 0

        // Активность была, но теперь сброшена в 0 (для отмены)
        val wasResetToZero = !isCompleted && habit.currentValue > 0 && newCurrentValue == 0


        // --- 3. ЛОГИКА ДАТЫ И СТРИКОВ ---

        var newCurrentStreak = habit.currentStreak
        var newBestStreak = habit.bestStreak
        var newLastCompletedDate: Long? = habit.lastCompletedDate


        if (wasActiveToday && habit.lastCompletedDate == null) {
            // A) Стрик начинается (первая активность сегодня, ранее не было).
            // Проверяем, было ли выполнение вчера, используя lastCompletedDate из DB, а не локальный.
            val wasCompletedYesterday = habit.lastCompletedDate?.let { isYesterday(it) } ?: false
            newCurrentStreak = if (wasCompletedYesterday) habit.currentStreak + 1 else 1
            newBestStreak = max(newCurrentStreak, habit.bestStreak)
            newLastCompletedDate = today

        } else if (wasActiveToday && newLastCompletedDate == null) {
            // Активность есть (newCurrentValue > 0), но дата сброшена (новая привычка или reset)
            newLastCompletedDate = today
        } else if (wasResetToZero) {
            // B) Активность сброшена в 0 (Отмена последнего действия)
            newCurrentStreak = 0 // Сброс текущего стрика
            newLastCompletedDate = null
        }

        // ВАЖНО: Если lastCompletedDate уже установлен на сегодня, мы не меняем стрик
        // при добавлении прогресса (+2, +3 и т.д.), но обновляем lastCompletedDate,
        // чтобы TodayScreen корректно отображал статус.
        if (newLastCompletedDate == null && wasActiveToday) {
            newLastCompletedDate = today
        }

        // --- 4. ВОЗВРАТ ОБНОВЛЕННОЙ ПРИВЫЧКИ ---
        return habit.copy(
            lastCompletedDate = newLastCompletedDate,
            currentStreak = newCurrentStreak,
            bestStreak = newBestStreak,
            currentValue = newCurrentValue
        )
    }

    /*private fun calculateNewStreakState(habit: Habit, isCompleted: Boolean): Habit {
        if (isCompleted) {
                val today = System.currentTimeMillis()
                val wasCompletedYesterday = habit.lastCompletedDate?.let { isYesterday(it) } ?: false
                val newCurrentStreak = if (wasCompletedYesterday) habit.currentStreak + 1 else 1
                val newBestStreak = max(newCurrentStreak, habit.bestStreak)
            return habit.copy(
                lastCompletedDate = today,
                currentStreak = newCurrentStreak,
                bestStreak = newBestStreak,
                currentValue = habit.targetValue
            )
        } else {
            val wasCompletedToday = isCompletedToday(habit)
            return habit.copy(
                lastCompletedDate = null,
                currentStreak = if (wasCompletedToday) habit.currentStreak - 1 else habit.currentStreak,
                currentValue = 0
            )
        }
    }*/

    private fun isCompletedToday(habit: Habit): Boolean {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return habit.lastCompletedDate != null && habit.lastCompletedDate!! >= todayStart
    }

    private fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val completionDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return yesterday.get(Calendar.YEAR) == completionDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == completionDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun calculateCompletionPercentage(
        completionDates: Set<LocalDate>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        if (startDate > endDate) return 0
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        if (totalDays <= 0) return 0

        val completedDaysCount = (0 until totalDays).count { offset ->
            val day = startDate.plusDays(offset.toLong())
            day in completionDates
        }
        val percentage = (completedDaysCount.toFloat() / totalDays.toFloat()) * 100
        return percentage.roundToInt().coerceIn(0, 100)
    }

    private fun buildWeekCalendar(
        completionDates: Set<LocalDate>,
        selectedDate: LocalDate
    ): List<CalendarDayState> {
        val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until 7).map { offset ->
            val day = startOfWeek.plusDays(offset.toLong())
            CalendarDayState(
                date = day,
                isCompleted = day in completionDates,
                isSelected = day == selectedDate
            )
        }
    }

    private fun formatWeekRange(selectedDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM")
        val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return "${startOfWeek.format(formatter)}–${endOfWeek.format(formatter)}"
    }

    private fun buildWeeklyTrend(
        habitsByDate: Map<LocalDate, List<Habit>>,
        totalHabitsCount: Int,
        referenceDate: LocalDate
    ): List<DayCompletion> {
        val startDate = referenceDate.minusDays(6)
        return (0 until 7).map { offset ->
            val day = startDate.plusDays(offset.toLong())
            val completedCount = habitsByDate[day]?.size ?: 0
            val ratio = if (totalHabitsCount == 0) 0f else completedCount.toFloat() / totalHabitsCount.toFloat()
            DayCompletion(date = day, completionRatio = ratio.coerceIn(0f, 1f))
        }
    }
}
