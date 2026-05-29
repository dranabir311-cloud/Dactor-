package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vitals_records")
data class VitalsRecord(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val heartRate: Int,
  val bloodPressureSystolic: Int,
  val bloodPressureDiastolic: Int,
  val bloodSugar: Int, // mg/dL
  val spo2: Int, // %
  val temperature: Double, // °F
  val timestamp: Long = System.currentTimeMillis(),
  val notes: String = ""
)

@Entity(tableName = "appointments")
data class Appointment(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val doctorName: String,
  val specialty: String,
  val date: String,
  val time: String,
  val reason: String,
  val status: String = "Scheduled", // "Scheduled", "Cancelled", "Completed"
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val role: String, // "user", "model"
  val messageText: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DactorDao {
  // Vitals
  @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC")
  fun getAllVitals(): Flow<List<VitalsRecord>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVitals(vitals: VitalsRecord)

  @Query("DELETE FROM vitals_records WHERE id = :id")
  suspend fun deleteVitalsById(id: Int)

  // Appointments
  @Query("SELECT * FROM appointments ORDER BY timestamp DESC")
  fun getAllAppointments(): Flow<List<Appointment>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAppointment(appointment: Appointment)

  @Query("UPDATE appointments SET status = :status WHERE id = :id")
  suspend fun updateAppointmentStatus(id: Int, status: String)

  @Query("DELETE FROM appointments WHERE id = :id")
  suspend fun deleteAppointmentById(id: Int)

  // Chat History
  @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
  fun getChatHistory(): Flow<List<ChatMessage>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChatMessage(message: ChatMessage)

  @Query("DELETE FROM chat_messages")
  suspend fun clearChatHistory()
}

@Database(entities = [VitalsRecord::class, Appointment::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class DactorDatabase : RoomDatabase() {
  abstract fun dactorDao(): DactorDao
}

object DatabaseProvider {
  private var db: DactorDatabase? = null

  fun getDatabase(context: Context): DactorDatabase {
    return db ?: synchronized(this) {
      val instance = Room.databaseBuilder(
        context.applicationContext,
        DactorDatabase::class.java,
        "dactor_database"
      )
      .fallbackToDestructiveMigration()
      .build()
      db = instance
      instance
    }
  }
}
