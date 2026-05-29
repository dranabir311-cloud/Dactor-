package com.example.data

import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DactorRepository(private val dactorDao: DactorDao) {

  // Vitals
  val allVitals: Flow<List<VitalsRecord>> = dactorDao.getAllVitals()

  suspend fun insertVitals(vitals: VitalsRecord) {
    dactorDao.insertVitals(vitals)
  }

  suspend fun deleteVitals(id: Int) {
    dactorDao.deleteVitalsById(id)
  }

  // Appointments
  val allAppointments: Flow<List<Appointment>> = dactorDao.getAllAppointments()

  suspend fun insertAppointment(appointment: Appointment) {
    dactorDao.insertAppointment(appointment)
  }

  suspend fun updateAppointmentStatus(id: Int, status: String) {
    dactorDao.updateAppointmentStatus(id, status)
  }

  suspend fun deleteAppointment(id: Int) {
    dactorDao.deleteAppointmentById(id)
  }

  // Chat History
  val chatHistory: Flow<List<ChatMessage>> = dactorDao.getChatHistory()

  suspend fun insertChatMessage(message: ChatMessage) {
    dactorDao.insertChatMessage(message)
  }

  suspend fun clearChat() {
    dactorDao.clearChatHistory()
  }

  // Handle consultation via Gemini API Model
  suspend fun generateConsultation(history: List<ChatMessage>, currentPrompt: String): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
      return "Hello! I am Dr. Dactor, your AI Wellness Companion. 🩺\n\nNotice: To enable live consultations with me, please enter your GEMINI_API_KEY in the AI Studio Secrets panel. This safe prototype currently uses a mock template."
    }

    // System instruction for Dr. Dactor
    val systemInstructionText = """
      You are Dr. Dactor, an empathetic, highly knowledgeable, and professional AI Medical Consultant.
      Your goal is to guide users to physical wellness, share healthy lifestyle tips (nutrition, sleep, exercise), and evaluate general symptoms they describe.
      
      Response Format:
      - Use professional, warm, empathetic language.
      - Keep explanations highly readable, short, structured with clean bullet points or small paragraphs.
      - For symptom inquiries, explain common gentle health triggers and suggest preventive measures.
      - ALWAYS end on a friendly warm health check message and the standard mandatory medical disclaimer:
        "Stay happy and healthy! Disclaimer: I am an AI companion, not a replacement for a certified physician or in-clinic physical examination."
      - STRICT ALERT: If the user mentions extreme symptoms (e.g., severe chest pains, choking, stroke indicators, massive bleeding, severe breathing difficulty), prioritize advising them immediately to call emergency medical services or visit the nearest ER, and omit normal recommendations.
    """.trimIndent()

    // Package current history for context
    val requestContents = mutableListOf<Content>()
    history.takeLast(10).forEach { msg ->
      val textPart = Part(text = "${if (msg.role == "user") "Patient" else "Dactor"}: ${msg.messageText}")
      requestContents.add(Content(parts = listOf(textPart)))
    }
    // Add current user prompt
    requestContents.add(Content(parts = listOf(Part(text = "Patient: $currentPrompt"))))

    val request = GenerateContentRequest(
      contents = requestContents,
      systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
    )

    return try {
      val response = RetrofitClient.service.generateContent(apiKey, request)
      response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        ?: "I appreciate your response. Could you explain your symptoms or wellness goal slightly differently?"
    } catch (e: Exception) {
      "Dr. Dactor is briefly offline. Connection error: ${e.localizedMessage}. Please verify your network and try again."
    }
  }
}
