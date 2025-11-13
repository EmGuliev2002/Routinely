package ru.routinely.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.routinely.app.data.AppDatabase
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.ui.theme.RoutinelyTheme
import ru.routinely.app.viewmodel.HabitViewModel
import ru.routinely.app.viewmodel.HabitViewModelFactory

/**
 * Класс, представляющий все возможные разделы приложения.
 * Используется как маршрут (route) для навигации.
 */
sealed class Screen(val route: String) {
    object Today : Screen("today")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
}


/**
 * Главная Activity, точка входа в приложение и контейнер для навигации.
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
                    // Корневой навигационный компонент
                    AppNavigation(habitViewModel = habitViewModel)
                }
            }
        }
    }
}

/**
 * Определяет структуру навигации приложения с Bottom Bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(habitViewModel: HabitViewModel) {
    val navController = rememberNavController()

    Scaffold(
        // Нижняя навигационная панель
        bottomBar = {
            RoutinelyBottomBar(navController = navController)
        }
    ) { paddingValues ->

        // Контейнер, который меняет содержимое в зависимости от выбранного раздела
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route, // Стартовый экран
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Раздел "Сегодня"
            composable(Screen.Today.route) {
                TodayScreen(habitViewModel = habitViewModel)
            }
            // Раздел "Статистика"
            composable(Screen.Stats.route) {
                // ИЗМЕНЕНИЕ: Теперь передаем ViewModel и onNavigateBack
                StatsScreen(
                    habitViewModel = habitViewModel,
                    onNavigateBack = { navController.popBackStack() } // PopBackStack вернет на предыдущий экран (Today)
                )
            }
            // Раздел "Настройки"
            composable(Screen.Settings.route) {
                // ИЗМЕНЕНИЕ: Теперь передаем ViewModel и onNavigateBack
                SettingsScreen(
                    habitViewModel = habitViewModel,
                    onNavigateBack = { navController.popBackStack() } // PopBackStack вернет на предыдущий экран (Today)
                )
            }
        }
    }
}