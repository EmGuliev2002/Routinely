package ru.routinely.app.utils

// Модель для управления состоянием сортировки
enum class SortOrder {
    BY_DATE,        // По дате создания
    BY_NAME,        // По имени (А-Я/Я-А)
    BY_STREAK       // По длине серии
}

// Модель для управления состоянием фильтрации
enum class HabitFilter {
    TODAY,          // Только на сегодня
    ALL,            // Все привычки
    UNCOMPLETED     // Только невыполненные
}