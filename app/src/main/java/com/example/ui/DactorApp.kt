package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Appointment
import com.example.data.ChatMessage
import com.example.data.VitalsRecord
import com.example.viewmodel.DactorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DactorApp(
  viewModel: DactorViewModel,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) }
  var showAddVitalsDialog by remember { mutableStateOf(false) }
  var showBookAppointmentDialog by remember { mutableStateOf(false) }

  val vitals by viewModel.vitalsState.collectAsStateWithLifecycle()
  val appointments by viewModel.appointmentsState.collectAsStateWithLifecycle()
  val chatHistory by viewModel.chatMessagesState.collectAsStateWithLifecycle()
  val isChatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  brush = Brush.linearGradient(
                    colors = listOf(
                      MaterialTheme.colorScheme.primary,
                      MaterialTheme.colorScheme.tertiary
                    )
                  ),
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              text = "Dactor",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleLarge
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              if (selectedTab == 0) showAddVitalsDialog = true
              else if (selectedTab == 2) showBookAppointmentDialog = true
            },
            enabled = selectedTab != 1,
            modifier = Modifier.testTag("action_add_button")
          ) {
            if (selectedTab != 1) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Add",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        NavigationBarItem(
          icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
          label = { Text("Dashboard") },
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          modifier = Modifier.testTag("nav_tab_dashboard")
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Favorite, contentDescription = "AI Consult") },
          label = { Text("Consult AI") },
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          modifier = Modifier.testTag("nav_tab_consult")
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.DateRange, contentDescription = "Appointments") },
          label = { Text("Appointments") },
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          modifier = Modifier.testTag("nav_tab_appointments")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
          fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "TabTransition"
      ) { targetTab ->
        when (targetTab) {
          0 -> DashboardScreen(
            vitals = vitals,
            onAddClick = { showAddVitalsDialog = true },
            onDeleteClick = { viewModel.deleteVitals(it) }
          )
          1 -> ConsultScreen(
            history = chatHistory,
            isLoading = isChatLoading,
            onSendMessage = { viewModel.sendChatMessage(it) },
            onClearHistory = { viewModel.clearChatHistory() }
          )
          2 -> AppointmentsScreen(
            appointments = appointments,
            onBookClick = { showBookAppointmentDialog = true },
            onCancelClick = { viewModel.cancelAppointment(it) },
            onDeleteClick = { viewModel.deleteAppointment(it) }
          )
        }
      }

      // Add Vitals Dialog
      if (showAddVitalsDialog) {
        AddVitalsDialog(
          onDismiss = { showAddVitalsDialog = false },
          onSave = { hr, sys, dia, sugar, spo2, temp, note ->
            viewModel.addVitals(hr, sys, dia, sugar, spo2, temp, note)
            showAddVitalsDialog = false
          }
        )
      }

      // Book Appointment Dialog
      if (showBookAppointmentDialog) {
        BookAppointmentDialog(
          onDismiss = { showBookAppointmentDialog = false },
          onBook = { doc, specialty, date, time, reason ->
            viewModel.bookAppointment(doc, specialty, date, time, reason)
            showBookAppointmentDialog = false
          }
        )
      }
    }
  }
}

// ======================== DASHBOARD SCREEN ========================

@Composable
fun DashboardScreen(
  vitals: List<VitalsRecord>,
  onAddClick: () -> Unit,
  onDeleteClick: (Int) -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Welcoming Header Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.secondary
            )
          )
        )
        .padding(24.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Welcome to Dactor Desk",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Healthy days ahead!",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
          Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Health Pulse",
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
          )
        }

        Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Your Wellness Index",
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "87 / 100",
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
    }

    // Interactive Core Vitals Grid
    val latestVital = vitals.firstOrNull()
    Text(
      text = "Latest Vitals Summary",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    if (latestVital != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        VitalGridItem(
          label = "Heart Rate",
          value = "${latestVital.heartRate} bpm",
          status = when {
            latestVital.heartRate in 60..100 -> "Normal"
            else -> "Irregular"
          },
          statusColor = when {
            latestVital.heartRate in 60..100 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
          },
          icon = Icons.Default.Favorite,
          modifier = Modifier.weight(1f)
        )

        VitalGridItem(
          label = "Blood Pressure",
          value = "${latestVital.bloodPressureSystolic}/${latestVital.bloodPressureDiastolic}",
          status = when {
            latestVital.bloodPressureSystolic < 130 && latestVital.bloodPressureDiastolic < 85 -> "Normal"
            else -> "High"
          },
          statusColor = when {
            latestVital.bloodPressureSystolic < 130 && latestVital.bloodPressureDiastolic < 85 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
          },
          icon = Icons.Default.Info,
          modifier = Modifier.weight(1f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        VitalGridItem(
          label = "Oxygen (SpO2)",
          value = "${latestVital.spo2}%",
          status = when {
            latestVital.spo2 >= 95 -> "Normal"
            else -> "Low Oxygen"
          },
          statusColor = when {
            latestVital.spo2 >= 95 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
          },
          icon = Icons.Default.Home,
          modifier = Modifier.weight(1f)
        )

        VitalGridItem(
          label = "Temperature",
          value = "${latestVital.temperature} °F",
          status = when {
            latestVital.temperature in 97.0..99.5 -> "Healthy"
            else -> "Fever Alert"
          },
          statusColor = when {
            latestVital.temperature in 97.0..99.5 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
          },
          icon = Icons.Default.Warning,
          modifier = Modifier.weight(1f)
        )
      }
    } else {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
          )
          Text(
            text = "No vitals logged yet",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
          )
          Text(
            text = "Log heart rate and oxygen levels to track diagnostic trends.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
          )
          Button(onClick = onAddClick) {
            Text("Add Vitals Now")
          }
        }
      }
    }

    // Dynamic Connected Line Chart for Heart Rate History
    if (vitals.isNotEmpty()) {
      Text(
        text = "Heart Rate Monitor Track",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          val graphPoints = vitals.takeLast(7).reversed()
          if (graphPoints.size > 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val width = size.width
              val height = size.height
              val maxVal = (graphPoints.maxOf { it.heartRate } + 10).toFloat()
              val minVal = (graphPoints.minOf { it.heartRate } - 10).coerceAtLeast(40).toFloat()
              val valueRange = maxVal - minVal

              val path = Path()
              val stepX = width / (graphPoints.size - 1)

              graphPoints.forEachIndexed { idx, v ->
                val ratioY = (v.heartRate - minVal) / valueRange
                val y = height - (ratioY * height)
                val x = idx * stepX

                if (idx == 0) {
                  path.moveTo(x, y)
                } else {
                  path.lineTo(x, y)
                }

                // Draw point circle
                drawCircle(
                  color = Color(0xFF00ACC1),
                  radius = 4.dp.toPx(),
                  center = Offset(x, y)
                )
              }

              drawPath(
                path = path,
                color = Color(0xFF007E8E),
                style = Stroke(width = 3.dp.toPx())
              )
            }
          } else {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Log additional stats to render the cardiovascular line trend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }

    // Past Metric Bullet Logs
    Text(
      text = "Historical Logs",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    if (vitals.isNotEmpty()) {
      vitals.forEach { record ->
        HistoricalVitalsRow(
          record = record,
          onDelete = { onDeleteClick(record.id) }
        )
      }
    } else {
      Text(
        text = "No history records available.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }
  }
}

@Composable
fun VitalGridItem(
  label: String,
  value: String,
  status: String,
  statusColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .shadow(2.dp, RoundedCornerShape(18.dp)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
          fontWeight = FontWeight.Medium
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
          modifier = Modifier.size(20.dp)
        )
      }

      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(statusColor.copy(alpha = 0.12f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = status,
          color = statusColor,
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}

@Composable
fun HistoricalVitalsRow(
  record: VitalsRecord,
  onDelete: () -> Unit
) {
  val readableDate = remember(record.timestamp) {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    sdf.format(Date(record.timestamp))
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(12.dp)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }

        Column {
          Text(
            text = "Pulse: ${record.heartRate} bpm | Temp: ${record.temperature}°F",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "BP: ${record.bloodPressureSystolic}/${record.bloodPressureDiastolic} | SpO2: ${record.spo2}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
          )
          if (record.notes.isNotBlank()) {
            Text(
              text = record.notes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = readableDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.testTag("delete_vitals_${record.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete entry",
          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
      }
    }
  }
}

// ======================== CONSULT SCREEN (AI) ========================

@Composable
fun ConsultScreen(
  history: List<ChatMessage>,
  isLoading: Boolean,
  onSendMessage: (String) -> Unit,
  onClearHistory: () -> Unit
) {
  var chatInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(history.size, isLoading) {
    if (history.isNotEmpty() || isLoading) {
      listState.animateScrollToItem((history.size * 2).coerceAtLeast(0))
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Session Actions Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Consult AI Dactor",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Secure wellness advisory portal",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
      }

      TextButton(
        onClick = onClearHistory,
        modifier = Modifier.testTag("clear_chat_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Reset", style = MaterialTheme.typography.bodySmall)
      }
    }

    // Message History List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(history) { message ->
        val isUser = message.role == "user"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.82f)
              .wrapContentWidth(align = if (isUser) Alignment.End else Alignment.Start)
              .clip(
                RoundedCornerShape(
                  topStart = 16.dp,
                  topEnd = 16.dp,
                  bottomStart = if (isUser) 16.dp else 2.dp,
                  bottomEnd = if (isUser) 2.dp else 16.dp
                )
              )
              .background(
                if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
              )
              .shadow(if (isUser) 0.dp else 1.dp)
              .padding(14.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = if (isUser) "You" else "Dr. Dactor",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.tertiary
              )
              Text(
                text = message.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground
              )
            }
          }
        }
      }

      if (isLoading) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.Start
          ) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = MaterialTheme.colorScheme.primary,
                  strokeWidth = 2.dp
                )
                Text(
                  text = "Dr. Dactor is analyzing...",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
              }
            }
          }
        }
      }
    }

    // Input Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TextField(
        value = chatInput,
        onValueChange = { chatInput = it },
        placeholder = { Text("Ask something (e.g., nutrition plans, flu checks)") },
        modifier = Modifier
          .weight(1f)
          .testTag("chat_input_text_field"),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        maxLines = 4,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
      )

      IconButton(
        onClick = {
          if (chatInput.isNotBlank()) {
            onSendMessage(chatInput)
            chatInput = ""
          }
        },
        enabled = chatInput.isNotBlank() && !isLoading,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            if (chatInput.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
          )
          .testTag("chat_send_button")
      ) {
        Icon(
          imageVector = Icons.Default.Send,
          contentDescription = "Send Consultation",
          tint = if (chatInput.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
      }
    }
  }
}

// ======================== APPOINTMENTS BOOKING SCREEN ========================

@Composable
fun AppointmentsScreen(
  appointments: List<Appointment>,
  onBookClick: () -> Unit,
  onCancelClick: (Int) -> Unit,
  onDeleteClick: (Int) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Booking Prompt Banner
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(18.dp)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Schedule Consultation Desk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Book a video or in-person check.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
          )
        }
        Button(
          onClick = onBookClick,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("book_consult_button")
        ) {
          Text("Book")
        }
      }
    }

    Text(
      text = "Your Appointment Queue",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )

    if (appointments.isNotEmpty()) {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(appointments) { appointment ->
          AppointmentQueueCard(
            appointment = appointment,
            onCancel = { onCancelClick(appointment.id) },
            onDelete = { onDeleteClick(appointment.id) }
          )
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
          )
          Text(
            text = "Your schedule is clear",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
          )
          Text(
            text = "No appointments registered. Click Book to secure a specialist slot.",
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AppointmentQueueCard(
  appointment: Appointment,
  onCancel: () -> Unit,
  onDelete: () -> Unit
) {
  val isCancelled = appointment.status == "Cancelled"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(2.dp, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .background(
                if (isCancelled) Color.LightGray.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = if (isCancelled) Color.Gray else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }

          Column {
            Text(
              text = appointment.doctorName,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyLarge,
              color = if (isCancelled) Color.Gray else MaterialTheme.colorScheme.primary
            )
            Text(
              text = appointment.specialty,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
          }
        }

        // Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
              when (appointment.status) {
                "Scheduled" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                "Cancelled" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                else -> Color.LightGray
              }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = appointment.status,
            color = when (appointment.status) {
              "Scheduled" -> MaterialTheme.colorScheme.tertiary
              "Cancelled" -> MaterialTheme.colorScheme.error
              else -> Color.DarkGray
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = appointment.date,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Info, // custom standard
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = appointment.time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
        }
      }

      if (appointment.reason.isNotBlank()) {
        Text(
          text = "Note: ${appointment.reason}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isCancelled) {
          IconButton(
            onClick = onCancel,
            modifier = Modifier.testTag("cancel_appointment_${appointment.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel Schedule",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
          }
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.testTag("delete_appointment_${appointment.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Purge historical card",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
          )
        }
      }
    }
  }
}

// ======================== DIALOGS ========================

@Composable
fun AddVitalsDialog(
  onDismiss: () -> Unit,
  onSave: (heartRate: Int, systolic: Int, diastolic: Int, sugar: Int, spo2: Int, temp: Double, note: String) -> Unit
) {
  var hr by remember { mutableStateOf("72") }
  var sys by remember { mutableStateOf("120") }
  var dia by remember { mutableStateOf("80") }
  var sugar by remember { mutableStateOf("95") }
  var spo2 by remember { mutableStateOf("98") }
  var temp by remember { mutableStateOf("98.6") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Record Today Vitals", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Enter raw physiological counts. Dactor diagnostics relies on accurate metrics.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )

        OutlinedTextField(
          value = hr,
          onValueChange = { hr = it },
          label = { Text("Heart Rate (bpm)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("add_vitals_hr")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sys,
            onValueChange = { sys = it },
            label = { Text("Systolic BP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("add_vitals_systolic")
          )
          OutlinedTextField(
            value = dia,
            onValueChange = { dia = it },
            label = { Text("Diastolic BP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("add_vitals_diastolic")
          )
        }

        OutlinedTextField(
          value = sugar,
          onValueChange = { sugar = it },
          label = { Text("Blood Sugar (mg/dL)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("add_vitals_sugar")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = spo2,
            onValueChange = { spo2 = it },
            label = { Text("Oxygen SpO2 (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("add_vitals_spo2")
          )
          OutlinedTextField(
            value = temp,
            onValueChange = { temp = it },
            label = { Text("Temp (°F)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("add_vitals_temp")
          )
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (e.g. before fasting, headache)") },
          modifier = Modifier.fillMaxWidth().testTag("add_vitals_notes")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val calculatedHr = hr.toIntOrNull() ?: 72
          val calculatedSys = sys.toIntOrNull() ?: 120
          val calculatedDia = dia.toIntOrNull() ?: 80
          val calculatedSugar = sugar.toIntOrNull() ?: 95
          val calculatedSpo2 = spo2.toIntOrNull() ?: 98
          val calculatedTemp = temp.toDoubleOrNull() ?: 98.6
          onSave(calculatedHr, calculatedSys, calculatedDia, calculatedSugar, calculatedSpo2, calculatedTemp, notes)
        },
        modifier = Modifier.testTag("save_vitals_button")
      ) {
        Text("Save Vitals")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun BookAppointmentDialog(
  onDismiss: () -> Unit,
  onBook: (doctorName: String, specialty: String, date: String, time: String, reason: String) -> Unit
) {
  val specialists = listOf(
    "Dr. Anita Sharma" to "Cardiology Specialist",
    "Dr. Rajiv Patel" to "General Practitioner",
    "Dr. Sarah Jenkins" to "Neurologist",
    "Dr. Michael Chang" to "Pediatrician"
  )

  var selectedIndex by remember { mutableStateOf(0) }
  var dateStr by remember { mutableStateOf("2026-06-01") }
  var timeStr by remember { mutableStateOf("11:00 AM") }
  var reason by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Secure Physician Slot", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Select one of our highly certified practitioners to conduct clinical profile consultations.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )

        Text(
          text = "Available Specialist:",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        specialists.forEachIndexed { index, pair ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (selectedIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
              )
              .clickable { selectedIndex = index }
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            RadioButton(
              selected = selectedIndex == index,
              onClick = { selectedIndex = index },
              modifier = Modifier.testTag("specialist_option_$index")
            )
            Column {
              Text(pair.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              Text(pair.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
            }
          }
        }

        OutlinedTextField(
          value = dateStr,
          onValueChange = { dateStr = it },
          label = { Text("Consultation Date (YYYY-MM-DD)") },
          modifier = Modifier.fillMaxWidth().testTag("appointment_date_field")
        )

        OutlinedTextField(
          value = timeStr,
          onValueChange = { timeStr = it },
          label = { Text("Consultation Time (e.g. 02:00 PM)") },
          modifier = Modifier.fillMaxWidth().testTag("appointment_time_field")
        )

        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason for visit / symptoms info") },
          modifier = Modifier.fillMaxWidth().testTag("appointment_reason_field")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val (doctorName, specialty) = specialists[selectedIndex]
          onBook(doctorName, specialty, dateStr, timeStr, reason)
        },
        modifier = Modifier.testTag("confirm_booking_button")
      ) {
        Text("Confirm Book")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
