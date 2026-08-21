package com.eduk.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val gradeLevel: Int,
    val difficulty: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val sourceMaterial: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val age: Int,
    val grade: Int,
    val country: String = "USA",
    val language: String = "English",
    val dailyTimeLimitMinutes: Int = 60,
    val timeEarnedMinutes: Int = 120,
    val accuracy: Int = 85,
    val isProtected: Boolean = true
)

@Entity(tableName = "usage_stats")
data class UsageStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val questionsAnswered: Int,
    val correctAnswers: Int,
    val timeUnlockedMinutes: Int,
    val topSubject: String
)
