package ru.routinely.app.ui

import HabitItem
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routinely.app.data.AppDatabase
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit
import ru.routinely.app.ui.theme.RoutinelyTheme
import ru.routinely.app.viewmodel.HabitViewModel
import ru.routinely.app.viewmodel.HabitViewModelFactory

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Главная Activity, точка входа в приложение.
 */
class MainActivity : ComponentActivity() {

    // Ручная инъекция зависимостей
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { HabitRepository(database.habitDao()) }

    // ViewModel инстанс
    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoutinelyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HabitScreen(habitViewModel = habitViewModel)
                }
            }
        }
    }
}


/**
 * Stateful-компонент уровня экрана (Контейнер).
 * Получает состояние из ViewModel и передает его в Stateless-компонент (HomeContent).
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(habitViewModel: HabitViewModel) {

    // Состояние для управления видимостью Bottom Sheet
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Подписываемся на состояние из ViewModel.
    val allHabits by habitViewModel.allHabits.observeAsState(initial = emptyList())

    // 1. Главный экран (HomeContent)
    HomeContent(
        habits = allHabits,
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
            onDismissRequest = { isSheetOpen = false }, // Закрытие по свайпу или клику вне
            sheetState = sheetState
        ) {
            // Размещаем здесь наш экран добавления привычки
            AddHabitScreen(
                viewModel = habitViewModel,
                onNavigateBack = {
                    isSheetOpen = false // Закрываем при нажатии "Назад" или "Сохранить"
                }
            )
        }
    }
}


/**
 * Stateless-компонент, отвечающий за отображение UI главного экрана.
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
        // Кнопка добавления внизу по центру, как на макете
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

// --- Вспомогательные Composable-функции ---

// Верхняя панель (Header)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Routinely",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Открыть настройки */ }) {
                Icon(Icons.Default.Person, contentDescription = "Профиль")
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Открыть меню/статистику */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// Кнопка добавления привычки
@Composable
fun AddHabitButton(onClick: () -> Unit) {
    // В дизайне используется широкая круглая кнопка, а не стандартный FAB
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f) // Занимает 80% ширины
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFB88EFA).copy(alpha = 0.85f)) // Используем фирменный фиолетовый
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

