package com.eduk.app.data

import android.content.Context
import androidx.room.*
import com.eduk.app.model.Question
import com.eduk.app.model.StudentProfile
import com.eduk.app.model.UsageStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE subject = :subject AND gradeLevel = :grade ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuestion(subject: String, grade: Int): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<Question>>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getProfile(): Flow<StudentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: StudentProfile)
}

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListString(list: List<String>): String {
        return Gson().toJson(list)
    }
}

@Database(entities = [Question::class, StudentProfile::class, UsageStats::class], version = 1)
@TypeConverters(Converters::class)
abstract class EdukDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: EdukDatabase? = null

        fun getDatabase(context: Context): EdukDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EdukDatabase::class.java,
                    "eduk_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
