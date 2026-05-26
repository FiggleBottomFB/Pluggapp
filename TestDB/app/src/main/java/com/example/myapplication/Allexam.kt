package com.example.myapplication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch


import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import java.time.LocalDate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import java.time.temporal.ChronoUnit

class Allexam : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "exam_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        enableEdgeToEdge()

        setContent {

            MyApplicationTheme {
                var showAddExam by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                var exams by remember {
                    mutableStateOf<List<Exam>>(emptyList())
                }


                // Load exams once
                LaunchedEffect(Unit) {
                    exams = db.examDao().getAll()
                }

                // call db and functions
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {

                        Button(
                            onClick = { showAddExam = true },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text("Lägg till prov")
                        }

                        if (showAddExam) {
                            DisplayAddExam(
                                onAdd = { exam ->
                                    scope.launch {
                                        db.examDao().insert(exam)
                                        exams = db.examDao().getAll()
                                    }
                                    showAddExam = false
                                },
                                onClose = {
                                    showAddExam = false
                                }
                            )
                        }

                        if (exams.isNotEmpty()) {
                            ExamList(
                                exams = exams,
                                onDelete = { exam ->
                                    scope.launch {
                                        db.examDao().delete(exam.id)
                                        exams = db.examDao().getAll()
                                    }
                                },
                                onOpen = { exam ->
                                    val intent = Intent(
                                        this@Allexam,
                                        ExamInfo::class.java
                                    )

                                    intent.putExtra("exam_id", exam.id)

                                    startActivity(intent)

                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// opens the add exam dialog
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayAddExam(onAdd: (Exam) -> Unit, onClose: () -> Unit) {

    val subjects = listOf(
        "Matte",
        "Fysik",
        "Programmering"
    )
    var selectedSubject by remember {
        mutableStateOf(subjects[0])
    }
    var expanded by remember {
        mutableStateOf(false)
    }
    var newName by remember {
        mutableStateOf("")
    }
    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    Dialog(onDismissRequest = {  onClose() }) {

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Box(
                modifier = Modifier.padding(20.dp)
            ) {

                IconButton(onClick = { onClose() }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("✕")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {

                    Text(
                        text = "Lägg till prov",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Exam name
                    TextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                        },
                        label = {
                            Text("Provnamn")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject dropdown
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = selectedSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Ämne")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            }
                        ) {

                            subjects.forEach { subject ->

                                DropdownMenuItem(
                                    text = {
                                        Text(subject)
                                    },
                                    onClick = {

                                        selectedSubject = subject
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DatePickerExample(
                        selectedDate = selectedDate,
                        onDateSelected = {
                            selectedDate = it
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onAdd(
                                Exam(
                                    name = newName,
                                    subject = selectedSubject,
                                    date = selectedDate
                                )
                            )
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lägg till prov")
                    }
                }
            }
        }
    }
}

// opens the date picker dialog
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerExample(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    val datePickerState = rememberDatePickerState()

    Button(
        onClick = {
            showDialog = true
        }
    ) {
        Text("Välj Datum")
    }

    Text("Valt datum: $selectedDate")

    if (showDialog) {

        DatePickerDialog(
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {

                Button(
                    onClick = {

                        val millis =
                            datePickerState.selectedDateMillis

                        if (millis != null) {

                            val localDate =
                                java.time.Instant
                                    .ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()

                            onDateSelected(localDate)
                        }

                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }
}

// Visar kortet för en exam
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExamList(exams: List<Exam>, onDelete: (Exam) -> Unit, modifier: Modifier = Modifier, onOpen: (Exam) -> Unit) {
    LazyColumn(
        modifier = modifier
    ) {

        items(exams) { exam ->

            // Calculate remaining days
            val daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(),
                exam.date
            )

            // Pick color based on remaining time
            val timeColor = when {
                daysLeft <= 3 -> Color.Red
                daysLeft <= 7 -> Color(0xFFFF9800)
                daysLeft <= 14 -> Color(0xFFC4B836)
                else -> Color(0xFF4CAF50)
            }

            if(daysLeft > -1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                    , onClick = {
                        onOpen(exam)
                    }
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "${exam.subject} • ${exam.date}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Remaining time text
                        Text(
                            text = when {
                                daysLeft == 0L ->
                                    "Provet är idag"

                                daysLeft == 1L ->
                                    "1 dag kvar"

                                else ->
                                    "$daysLeft dagar kvar"
                            },
                            color = timeColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Button(
                            onClick = {
                                onDelete(exam)
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Ta Bort")
                        }
                    }
                }
            }
        }
    }
}