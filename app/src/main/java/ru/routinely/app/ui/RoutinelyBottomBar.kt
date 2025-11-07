package ru.routinely.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.routinely.app.R // Замените на R.string.X, если используете ресурсы

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val items = listOf(
    NavItem("Сегодня", Icons.Default.Home, Screen.Today.route),
    NavItem("Статистика", Icons.Default.BarChart, Screen.Stats.route),
    NavItem("Настройки", Icons.Default.Settings, Screen.Settings.route)
)

@Composable
fun RoutinelyBottomBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        // Избегаем создания большого стека мест назначения при повторном выборе элемента
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Избегаем нескольких копий одного и того же места назначения
                        launchSingleTop = true
                        // Восстанавливаем предыдущее состояние при повторном выборе
                        restoreState = true
                    }
                }
            )
        }
    }
}