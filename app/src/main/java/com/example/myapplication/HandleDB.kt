package com.example.myapplication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

import java.time.LocalDate

import androidx.room.TypeConverters
import kotlinx.serialization.Serializable


@Serializable
data class OnlineStudySet(
    val id: Long? = null,
    val name: String,
    val subject: String,
    val description: String
)
@Serializable
data class OnlineStudySetConn(
    val id: Long? = null,
    val term: String,
    val definition: String,
    val studyset_id: Long
)

@RequiresApi(Build.VERSION_CODES.O)
@Entity
data class Exam @RequiresApi(Build.VERSION_CODES.O) constructor(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now()
)

@Dao
interface ExamDao {
    @Insert
    suspend fun insert(exam: Exam)

    @Query("DELETE FROM Exam Where id = :inputID")
    suspend fun delete(inputID: Int)

    @Query("SELECT * FROM Exam ORDER BY date ASC")
    suspend fun getAll(): List<Exam>

    @Query("SELECT * FROM Exam WHERE id = :inputID LIMIT 1")
    suspend fun getExam(inputID: Int): Exam

    @Query("UPDATE Exam SET name = :updateName, subject = :updateSubject, date = :updateDate, description = :updateDescription WHERE id = :inputID")
    suspend fun updateExam(inputID: Int, updateName: String, updateSubject: String, updateDate: LocalDate, updateDescription: String)
}


@Database(entities = [Exam::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
}

@Entity
data class StudySet constructor(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val name: String = "",
    val subject: String = "",
    val description: String = ""
)

@Entity
data class StudySetConn constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studySetId: Int = 0,
    val term: String = "",
    val definition: String = ""
)

@Dao
interface StudySetDao {
    @Insert
    suspend fun insert(studyset: StudySet): Long

    @Query("DELETE FROM StudySet Where id = :inputID")
    suspend fun delete(inputID: Int)

    @Query("SELECT * FROM StudySet")
    suspend fun getAll(): List<StudySet>

    @Query("SELECT * FROM StudySet WHERE id = :inputID LIMIT 1")
    suspend fun getExam(inputID: Int): StudySet

    @Query("UPDATE StudySet SET name = :updateName, subject = :updateSubject, description = :updateDescription WHERE id = :inputID")
    suspend fun updateExam(inputID: Int, updateName: String, updateSubject: String, updateDescription: String)
}

@Dao
interface StudySetConnDao {
    @Query("INSERT INTO StudySetConn (StudySetId, term, definition) VALUES (:inputId, :inputTerm, :inputDefinition)")
    suspend fun insert(inputId: Int, inputTerm: String, inputDefinition: String)

    @Query("SELECT * FROM StudySetConn WHERE studySetId = :inputId")
    suspend fun getAll(inputId: Int): List<StudySetConn>
}


@Database(entities = [StudySet::class, StudySetConn::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class StudySetDatabase : RoomDatabase() {
    abstract fun studySetDao(): StudySetDao
    abstract fun studySetConnDao(): StudySetConnDao
}