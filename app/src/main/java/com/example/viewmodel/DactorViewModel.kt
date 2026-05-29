package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DactorViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: DactorRepository

  init {
    val database = DatabaseProvider.getDatabase(application)
    val dao = database.dactorDao()
    repository = DactorRepository(dao)

    // Prepopulate with a helpful welcome conversation if database is fresh
    viewModelScope.launch {
      dao.getChatHistory().first().let { history ->
        if (history.isEmpty()) {
          dao.insertChatMessage(
            ChatMessage(
              role = "model",
              messageText = "Hello! I am Dr. Dactor, your virtual wellness coordinator. 🩺✨\nHow can I support your health goals today? Feel free to ask me questions about medical metrics, nutrition plans, custom diets, or describe symptoms you'd like general guidance on."
            )
          )
        }
      }

      // Prepopulate dummy schedules for rich visualization in UI
      dao.getAllAppointments().first().let { appointments ->
        if (appointments.isEmpty()) {
          dao.insertAppointment(
            Appointment(
              doctorName = "Dr. Anita Sharma",
              specialty = "Cardiology Specialist",
              date = "2026-06-15",
              time = "10:30 AM",
              reason = "Cardiovascular wellness checkup.",
              status = "Scheduled"
            )
          )
          dao.insertAppointment(
            Appointment(
              doctorName = "Dr. Rajiv Patel",
              specialty = "General Practitioner",
              date = "2026-05-30",
              time = "02:00 PM",
              reason = "Comprehensive health profile review.",
              status = "Scheduled"
            )
          )
        }
      }

      // Prepopulate historical vitals
      dao.getAllVitals().first().let { vitalsList ->
        if (vitalsList.isEmpty()) {
          // Add a healthy baseline card
          dao.insertVitals(
            VitalsRecord(
              heartRate = 72,
              bloodPressureSystolic = 120,
              bloodPressureDiastolic = 80,
              bloodSugar = 95,
              spo2 = 99,
              temperature = 98.6,
              notes = "Healthy baseline check."
            )
          )
        }
      }
    }
  }

  val vitalsState: StateFlow<List<VitalsRecord>> = repository.allVitals
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appointmentsState: StateFlow<List<Appointment>> = repository.allAppointments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val chatMessagesState: StateFlow<List<ChatMessage>> = repository.chatHistory
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _chatLoading = MutableStateFlow(false)
  val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

  fun addVitals(
    heartRate: Int,
    systolic: Int,
    diastolic: Int,
    bloodSugar: Int,
    spo2: Int,
    temperature: Double,
    notes: String
  ) {
    viewModelScope.launch {
      val record = VitalsRecord(
        heartRate = heartRate,
        bloodPressureSystolic = systolic,
        bloodPressureDiastolic = diastolic,
        bloodSugar = bloodSugar,
        spo2 = spo2,
        temperature = temperature,
        notes = notes
      )
      repository.insertVitals(record)
    }
  }

  fun deleteVitals(id: Int) {
    viewModelScope.launch {
      repository.deleteVitals(id)
    }
  }

  fun bookAppointment(
    doctorName: String,
    specialty: String,
    date: String,
    time: String,
    reason: String
  ) {
    viewModelScope.launch {
      val appointment = Appointment(
        doctorName = doctorName,
        specialty = specialty,
        date = date,
        time = time,
        reason = reason
      )
      repository.insertAppointment(appointment)
    }
  }

  fun cancelAppointment(id: Int) {
    viewModelScope.launch {
      repository.updateAppointmentStatus(id, "Cancelled")
    }
  }

  fun deleteAppointment(id: Int) {
    viewModelScope.launch {
      repository.deleteAppointment(id)
    }
  }

  fun sendChatMessage(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      // 1. Insert user message in DB
      val userMessage = ChatMessage(role = "user", messageText = text)
      repository.insertChatMessage(userMessage)

      _chatLoading.value = true

      // 2. Fetch history up to this point
      val history = repository.chatHistory.first()

      // 3. Request Gemini AI Model
      val responseText = repository.generateConsultation(history, text)

      // 4. Save model output in DB
      val modelMessage = ChatMessage(role = "model", messageText = responseText)
      repository.insertChatMessage(modelMessage)

      _chatLoading.value = false
    }
  }

  fun clearChatHistory() {
    viewModelScope.launch {
      repository.clearChat()
      // Insert initial hello
      repository.insertChatMessage(
        ChatMessage(
          role = "model",
          messageText = "Hello! I am Dr. Dactor, your virtual wellness coordinator. 🩺✨\nHow can I support your health goals today? Feel free to ask or describe symptoms."
        )
      )
    }
  }
}
