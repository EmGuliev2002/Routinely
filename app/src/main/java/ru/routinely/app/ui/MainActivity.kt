package ru.routinely.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.routinely.app.data.AppDatabase
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit
import ru.routinely.app.model.HabitType
import ru.routinely.app.ui.theme.RoutinelyTheme
import ru.routinely.app.viewmodel.HabitViewModel
import ru.routinely.app.viewmodel.HabitViewModelFactory

/**
 * Главная Activity, точка входа в приложение.
 * Отвечает за инициализацию архитектурных компонентов и установку корневого Composable.
 */
class MainActivity : ComponentActivity() {

    // Ручная инъекция зависимостей: создаем и передаем необходимые экземпляры.
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { HabitRepository(database.habitDao()) }
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
 * Stateful-компонент уровня экрана.
 * Его задача - подключиться к ViewModel, получить состояние и передать его
 * в Stateless-компонент для отрисовки (State Hoisting).
 */
@Composable
fun HabitScreen(habitViewModel: HabitViewModel) {
    // Подписываемся на состояние из ViewModel. При изменении данных, `allHabits`
    // обновится, что вызовет рекомпозицию.
    val allHabits by habitViewModel.allHabits.observeAsState(initial = emptyList())

    // Передаем текущее состояние в презентационный компонент.
    HabitListContent(habits = allHabits)
}


/**
 * Stateless-компонент, отвечающий исключительно за отображение UI.
 * Не содержит логики и не зависит от ViewModel.
 */
@Composable
fun HabitListContent(habits: List<Habit>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        if (habits.isEmpty()) {
            Text(text = "Привычек пока нет. Добавьте первую!")
        } else {
            Text(text = "Всего привычек: ${habits.size}")
            habits.forEach { habit ->
                Text(text = "- ${habit.name}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
