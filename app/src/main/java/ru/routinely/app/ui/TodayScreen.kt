package ru.routinely.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import ru.routinely.app.model.HabitCompletion
import ru.routinely.app.utils.HabitFilter
import ru.routinely.app.utils.SortOrder
import ru.routinely.app.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(habitViewModel: HabitViewModel) {
    // --- Состояния UI ---
    // Управление BottomSheet (шторкой)
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Привычка для редактирования (если null - создаем новую)
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    // Привычка для изменения прогресса через слайдер
    var habitForProgress by remember { mutableStateOf<Habit?>(null) }

    // --- Данные из ViewModel ---
    val uiState by habitViewModel.uiState.collectAsState()
    val completions by habitViewModel.completions.collectAsState()

    // --- Логика даты и группировки ---
    // Начало сегодняшнего дня в миллисекундах
    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Группируем выполнения по ID привычки для быстрого поиска
    val completionsByHabit = completions.groupBy { it.habitId }

    // Сортировка списка для отображения:
    // 1. Применяем сортировку из настроек (uiState.habits уже отсортированы ViewModel).
    // 2. Дополнительно: невыполненные сегодня показываем выше выполненных.
    val habitsForDisplay = uiState.habits.sortedWith(
        compareBy { habit ->
            // true (выполнено) будет ниже, false (не выполнено) выше
            completionsByHabit[habit.id]?.any { it.completionDay == todayStart } == true
        }
    )

    // --- Диалоговое окно со слайдером ---
    if (habitForProgress != null) {
        HabitProgressDialog(
            habit = habitForProgress!!,
            onDismiss = { habitForProgress = null },
            onConfirm = { newValue ->
                habitViewModel.updateHabitProgress(habitForProgress!!, newValue)
                habitForProgress = null
            }
        )
    }

    // --- Основной контент экрана ---
    HomeContent(
        habits = habitsForDisplay,
        viewModel = habitViewModel,
        completionsByHabit = completionsByHabit,
        todayStart = todayStart,
        onHabitClick = { habit, isCompletedToday ->
            // ЛОГИКА ОБЫЧНОГО КЛИКА
            if (habit.targetValue >= 5) {
                // Если цель большая (страницы, километры) -> открываем слайдер
                habitForProgress = habit
            } else {
                // Если простая привычка (да/нет) -> просто переключаем статус
                habitViewModel.onHabitCheckedChanged(habit, !isCompletedToday)
            }
        },
        onHabitLongClick = { habit ->
            // ЛОГИКА ДОЛГОГО НАЖАТИЯ -> Редактирование
            habitToEdit = habit
            isSheetOpen = true
        },
        onAddHabitClick = {
            // КЛИК ПО FAB -> Создание новой
            habitToEdit = null
            isSheetOpen = true
        }
    )

    // --- Нижняя шторка (BottomSheet) ---
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddHabitScreen(
                viewModel = habitViewModel,
                habitToEdit = habitToEdit, // Передаем привычку (или null)
                onNavigateBack = { isSheetOpen = false }
            )
        }
    }
}

/**
 * Внутренний компонент для отрисовки списка и FAB.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    habits: List<Habit>,
    viewModel: HabitViewModel,
    completionsByHabit: Map<Int, List<HabitCompletion>>,
    todayStart: Long,
    onHabitClick: (Habit, Boolean) -> Unit,
    onHabitLongClick: (Habit) -> Unit,
    onAddHabitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { AppTopBar(viewModel = viewModel) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp), // Отступ снизу, чтобы FAB не перекрывал
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (habits.isEmpty()) {
                item {
                    Text(
                        text = "Привычек пока нет. Нажмите '+' для добавления.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    // Вычисление статуса выполнения для конкретной привычки
                    val isCompletedTodayVisually = completionsByHabit[habit.id]
                        ?.any { it.completionDay == todayStart } == true

                    // Оборачиваем HabitItem в Box для обработки кликов и длинных кликов
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onHabitClick(habit, isCompletedTodayVisually) },
                            onLongClick = { onHabitLongClick(habit) }
                        )
                    ) {
                        HabitItem(
                            habit = habit,
                            isCompletedToday = isCompletedTodayVisually,
                            onCheckedChange = {
                                // Обработка клика по чекбоксу/свайпу внутри элемента
                                onHabitClick(habit, isCompletedTodayVisually)
                            },
                            onItemClick = { } // Клик обработан в combinedClickable выше
                        )
                    }
                }
            }
        }
    }
}

/**
 * Верхняя панель приложения с меню сортировки и фильтрации.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: HabitViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    // Подписываемся на uiState, чтобы отображать текущие фильтры/категории
    val uiState by viewModel.uiState.collectAsState()

    TopAppBar(
        title = { Text("Сегодня") },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Меню"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // --- Секция Сортировки ---
                Text(
                    "Сортировка",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                DropdownMenuItem(
                    text = { Text("По дате создания") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.BY_DATE)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("По названию (А-Я / Я-А)") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.BY_NAME)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("По длине серии") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.BY_STREAK)
                        showMenu = false
                    }
                )

                Divider()

                // --- Секция Фильтрации ---
                Text(
                    "Фильтрация",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                DropdownMenuItem(
                    text = { Text("Только на сегодня") },
                    onClick = {
                        viewModel.setFilter(HabitFilter.TODAY)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Показать все") },
                    onClick = {
                        viewModel.setFilter(HabitFilter.ALL)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Только невыполненные") },
                    onClick = {
                        viewModel.setFilter(HabitFilter.UNCOMPLETED)
                        showMenu = false
                    }
                )

                // --- Секция Категорий (если они есть) ---
                if (uiState.categories.isNotEmpty()) {
                    Divider()
                    Text(
                        "Категории",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DropdownMenuItem(
                        text = { Text("Все категории") },
                        onClick = {
                            viewModel.setCategoryFilter(null)
                            showMenu = false
                        }
                    )
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                viewModel.setCategoryFilter(category)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}