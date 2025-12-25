package ru.routinely.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditCalendar // Иконка для пустого состояния
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    // Управление BottomSheet (шторкой) создания/редактирования
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Привычка для редактирования (если null - создаем новую)
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    // Привычка для изменения прогресса через слайдер (если targetValue >= 5)
    var habitForProgress by remember { mutableStateOf<Habit?>(null) }

    // Привычка для удаления (хранится пока виден диалог подтверждения)
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    // --- Данные из ViewModel ---
    val uiState by habitViewModel.uiState.collectAsState()
    val completions by habitViewModel.completions.collectAsState()

    // --- Логика даты и группировки ---
    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Группируем выполнения по ID привычки для быстрого поиска
    val completionsByHabit = completions.groupBy { it.habitId }

    // Сортировка списка для отображения:
    // 1. Применяем сортировку из настроек (uiState.habits уже отсортированы во ViewModel).
    // 2. Дополнительно: невыполненные сегодня показываем ВЫШЕ выполненных.
    val habitsForDisplay = uiState.habits.sortedWith(
        compareBy { habit ->
            // true (выполнено) будет ниже (1), false (не выполнено) выше (0)
            if (completionsByHabit[habit.id]?.any { it.completionDay == todayStart } == true) 1 else 0
        }
    )

    // --- Диалоговое окно подтверждения удаления ---
    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Удалить привычку?") },
            text = { Text("Вы действительно хотите удалить привычку \"${habitToDelete?.name}\"? История выполнений также будет удалена.") },
            confirmButton = {
                Button(
                    onClick = {
                        habitToDelete?.let { habitViewModel.deleteHabit(it) }
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // --- Диалоговое окно со слайдером (для количественных привычек) ---
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

    // --- Основной контент экрана (Scaffold + List) ---
    HomeContent(
        habits = habitsForDisplay,
        viewModel = habitViewModel,
        completionsByHabit = completionsByHabit,
        todayStart = todayStart,
        onHabitClick = { habit, isCompletedToday ->
            // ЛОГИКА СВАЙПА ВПРАВО (Выполнить/Отменить)
            if (habit.targetValue >= 5) {
                // Если цель большая (страницы, километры) -> открываем слайдер
                habitForProgress = habit
            } else {
                // Если простая привычка (да/нет) -> просто переключаем статус
                habitViewModel.onHabitCheckedChanged(habit, !isCompletedToday)
            }
        },
        onHabitDelete = { habit ->
            // ЛОГИКА СВАЙПА ВЛЕВО -> Показываем диалог удаления
            habitToDelete = habit
        },
        onHabitEdit = { habit ->
            // ЛОГИКА КЛИКА ПО КАРТОЧКЕ -> Редактирование
            habitToEdit = habit
            isSheetOpen = true
        },
        onAddHabitClick = {
            // КЛИК ПО FAB -> Создание новой привычки
            habitToEdit = null
            isSheetOpen = true
        }
    )

    // --- Нижняя шторка (BottomSheet) для создания/редактирования ---
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddHabitScreen(
                viewModel = habitViewModel,
                habitToEdit = habitToEdit,
                onNavigateBack = { isSheetOpen = false }
            )
        }
    }
}

/**
 * Внутренний компонент для отрисовки Scaffold, Списка и FAB.
 * Содержит UX/UI улучшения (Empty State, Sticky Headers).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    habits: List<Habit>,
    viewModel: HabitViewModel,
    completionsByHabit: Map<Int, List<HabitCompletion>>,
    todayStart: Long,
    onHabitClick: (Habit, Boolean) -> Unit,
    onHabitDelete: (Habit) -> Unit,
    onHabitEdit: (Habit) -> Unit,
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
        // Кнопка строго по центру для удобства
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            // Добавляем отступ снизу (bottom = 80.dp), чтобы FAB не перекрывал последнюю карточку
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (habits.isEmpty()) {
                // --- 1. Empty State (Красивое состояние "Пусто") ---
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize() // Занимает весь экран
                            .padding(bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "Список привычек пуст",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите «+», чтобы создать\nсвою первую полезную привычку!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // --- 2. ГРУППИРОВКА (Умные заголовки) ---

                // Группируем привычки по времени
                val groupedHabits = habits.groupBy { habit ->
                    getDayTimeCategory(habit.notificationTime)
                }.toSortedMap(compareBy { getCategorySortOrder(it) })

                groupedHabits.forEach { (category, categoryHabits) ->
                    // Заголовок группы (прилипает к верху)
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background // Фон обязателен, чтобы перекрывать прокручиваемый контент
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Элементы группы
                    items(categoryHabits, key = { it.id }) { habit ->
                        // Вычисляем статус: выполнена ли привычка сегодня
                        val isCompletedTodayVisually = completionsByHabit[habit.id]
                            ?.any { it.completionDay == todayStart } == true

                        HabitItem(
                            habit = habit,
                            isCompletedToday = isCompletedTodayVisually,
                            onCheckedChange = { _ ->
                                onHabitClick(habit, isCompletedTodayVisually)
                            },
                            onDelete = {
                                onHabitDelete(habit)
                            },
                            onItemClick = {
                                onHabitEdit(habit)
                            }
                        )
                    }
                }

                // --- 3. Swipe Hint (Подсказка для жестов) ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Смахните вправо для выполнения,\nвлево — для удаления",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Верхняя панель приложения с выпадающим меню сортировки и фильтрации.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: HabitViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    // Подписываемся на uiState, чтобы отображать текущие категории в меню
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
                HorizontalDivider()

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

                // --- Секция Категорий (рендерим только если есть хоть одна категория) ---
                if (uiState.categories.isNotEmpty()) {
                    HorizontalDivider()
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

// --- Хелперы для группировки (логика отображения) ---

private fun getDayTimeCategory(notificationTime: String?): String {
    if (notificationTime == null) return "В любое время"

    // Формат времени "HH:mm"
    val hour = try {
        notificationTime.split(":")[0].toInt()
    } catch (e: Exception) {
        return "В любое время"
    }

    return when (hour) {
        in 5..11 -> "Утро 🌅"
        in 12..16 -> "День ☀️"
        in 17..22 -> "Вечер 🌇"
        else -> "Ночь 🌙"
    }
}

private fun getCategorySortOrder(category: String): Int {
    return when (category) {
        "Утро 🌅" -> 1
        "День ☀️" -> 2
        "Вечер 🌇" -> 3
        "Ночь 🌙" -> 4
        else -> 5 // "В любое время" в конце
    }
}