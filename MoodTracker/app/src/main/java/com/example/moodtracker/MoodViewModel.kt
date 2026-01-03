package com.example.moodtracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MoodViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MoodDatabase.getDatabase(application)
    private val moodDao = database.moodDao()
    private val habitDao = database.habitDao()

    val moodList: StateFlow<List<MoodEntry>> = moodDao.getAllMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitList: StateFlow<List<Habit>> = habitDao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (habitDao.getCount() == 0) {
                val defaultHabits = listOf(
                    Habit(iconRes = R.drawable.img_wakeup_habit, title = "Wake Up Early", description = "Start the day...", points = 20),
                    Habit(iconRes = R.drawable.img_sleep_habit, title = "Sleep Early", description = "End the day gently...", points = 30),
                    Habit(iconRes = R.drawable.img_pray_habit, title = "Daily Pray", description = "Take your moment...", points = 20),
                    Habit(iconRes = R.drawable.img_exercise_habit, title = "Exercise", description = "Move your body...", points = 10),
                    Habit(iconRes = R.drawable.img_selfcare_habit, title = "Self-care", description = "Do your skincare...", points = 10),
                    Habit(iconRes = R.drawable.img_hobbies_habit, title = "Hobbies", description = "Spend time...", points = 10)
                )
                defaultHabits.forEach { habitDao.insertHabit(it) }
            }
        }
    }

    fun addMood(entry: MoodEntry) {
        viewModelScope.launch { moodDao.insertMood(entry) }
    }

    fun deleteMood(entry: MoodEntry) {
        viewModelScope.launch { moodDao.deleteMood(entry) }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch { habitDao.updateHabit(habit) }
    }

    fun checkAndResetStreak(habit: Habit) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = Calendar.getInstance().apply {
            timeInMillis = if(habit.lastCompletedDate == 0L) 0L else habit.lastCompletedDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffDays = TimeUnit.MILLISECONDS.toDays(today - lastDate)

        if (habit.lastCompletedDate != 0L) {
            if (diffDays == 1L) {
                if (habit.isCompleted) {
                    updateHabit(habit.copy(isCompleted = false))
                }
            } else if (diffDays > 1L) {
                updateHabit(habit.copy(isCompleted = false, streak = 0))
            }
        }
    }
}