package ru.routinely.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val notificationTime: String? = null,
    val category: String? = null,
    val type: String,
    val targetValue: Int = 1,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false,
    val creationDate: Long = System.currentTimeMillis()
)

object HabitType {
    const val DAILY = "DAILY"
    const val WEEKLY_DAYS = "WEEKLY_DAYS"
    const val CUSTOM_INTERVAL = "CUSTOM_INTERVAL"
}