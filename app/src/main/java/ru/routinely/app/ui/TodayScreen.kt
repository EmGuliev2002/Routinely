package ru.routinely.app.ui

// ВАЖНО: Добавьте сюда все импорты, которые были в MainActivity
import HabitItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import ru.routinely.app.viewmodel.HabitViewModel
import java.util.Calendar


/**
 * Stateful-компонент главного экрана (раздел "Сегодня").
 * Содержит логику сортировки и управляет Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(habitViewModel: HabitViewModel) {
    // Состояние для управления видимостью Bottom Sheet
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Подписываемся на состояние из ViewModel.
    val habitsForTodayList by habitViewModel.habitsForToday.observeAsState(initial = emptyList())

    // --- СОРТИРОВКА: Невыполненные (false) идут перед Выполненными (true) ---
    val sortedHabits = habitsForTodayList.sortedWith(
        compareBy { habit ->
            // Логика проверки выполнения на сегодня
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val isCompleted = habit.lastCompletedDate != null && habit.lastCompletedDate >= todayStart
            isCompleted
        }
    )

    // 1. Главный экран (HomeContent)
    HomeContent(
        habits = sortedHabits,
        onHabitCheckedChange = { habit, isChecked ->
            habitViewModel.onHabitCheckedChanged(habit, isChecked)
        },
        onAddHabitClick = {
            isSheetOpen = true // Открываем Bottom Sheet при нажатии '+'
        }
    )

    // 2. Bottom Sheet (Выезжающее окно)
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddHabitScreen(
                viewModel = habitViewModel,
                onNavigateBack = { isSheetOpen = false }
            )
        }
    }
}

/**
 * Stateless-компонент, отвечающий за отображение UI списка привычек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    habits: List<Habit>,
    onHabitCheckedChange: (Habit, Boolean) -> Unit,
    onAddHabitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { AppTopBar() },
        floatingActionButton = { AddHabitButton(onAddHabitClick) },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->

        // Список привычек
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (habits.isEmpty()) {
                item {
                    Text(
                        text = "Привычек пока нет. Нажмите '+' для добавления.",
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitItem(
                        habit = habit,
                        onCheckedChange = { isChecked -> onHabitCheckedChange(habit, isChecked) }
                    )
                }
            }
        }
    }
}

// Верхняя панель (Header)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Сегодня",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Person, contentDescription = "Профиль")
            }
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// Кнопка добавления привычки
@Composable
fun AddHabitButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFB88EFA).copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Добавить привычку",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}