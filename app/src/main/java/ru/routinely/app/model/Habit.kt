package ru.routinely.app.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Модель данных (Entity), представляющая одну привычку в базе данных.
 *
 * @param id Уникальный идентификатор привычки, генерируется автоматически.
 * @param name Название привычки, которое вводит пользователь.
 * @param icon Идентификатор иконки для визуального представления.
 * @param color HEX-код цвета для кастомизации элемента списка.
 * @param notificationTime Время для ежедневного уведомления (например, "HH:mm"). Null, если не установлено.
 * @param category Категория привычки (например, "Спорт", "Образование").
 * @param type Тип привычки, определяет логику ее отображения (ежедневная, по дням и т.д.).
 * @param targetValue Целевое значение для привычек с прогрессом (например, 15 страниц).
 * @param currentValue Текущее выполненное значение (для прогресс-баров).
 * @param creationDate Временная метка создания привычки для сортировки.
 * @param lastCompletedDate Временная метка последнего выполнения. Ключевое поле для логики стриков.
 * @param currentStreak Текущая непрерывная серия выполнений.
 * @param bestStreak Лучшая непрерывная серия выполнений за все время.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_id")
    val icon: String? = null,

    @ColumnInfo(name = "color_hex")
    val color: String? = null,

    @ColumnInfo(name = "notification_time")
    val notificationTime: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "target_value")
    val targetValue: Int = 1,

    @ColumnInfo(name = "current_value")
    val currentValue: Int = 0,

    @ColumnInfo(name = "creation_date")
    val creationDate: Long = System.currentTimeMillis(),

    // --- Поля для логики стриков и статистики ---

    @ColumnInfo(name = "last_completed_date")
    val lastCompletedDate: Long? = null,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0
)

/**
 * Объект-константа для типов привычек.
 * Позволяет избежать "магических строк" в коде.
 */
object HabitType {
    const val DAILY = "DAILY" // Ежедневная
    const val WEEKLY_DAYS = "WEEKLY_DAYS" // По определенным дням недели (Пн, Ср)
    const val CUSTOM_INTERVAL = "CUSTOM_INTERVAL" // С интервалом (раз в 2 дня)
}