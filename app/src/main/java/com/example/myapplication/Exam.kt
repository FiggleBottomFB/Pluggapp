package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp

import java.time.LocalDate

class ExamInfo : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "exam_database"
        )            .fallbackToDestructiveMigration()
            .build()

        val examId = intent.getIntExtra("exam_id", -1)

        setContent {

            val scope = rememberCoroutineScope()
            var currentexam by remember {
                mutableStateOf<Exam?>(null)
            }

            LaunchedEffect(Unit) {
                currentexam = db.examDao().getExam(examId)
            }
            Scaffold() {
                Column() {
                    Row(modifier = Modifier.padding(top = 40.dp, start = 10.dp, end = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = {
                            val intent = Intent(
                                this@ExamInfo,
                                Allexam::class.java
                            )
                            startActivity(intent)
                        }) { Text("Tillbaka") }
                    }
                    currentexam?.let {
                        DisplayExam(
                            it.id,
                            it.name,
                            it.subject,
                            it.date.toString(),
                            it.description,
                            onUpdate = { exam ->
                                scope.launch {
                                    db.examDao().updateExam(exam.id, exam.name, exam.subject, exam.date, exam.description)
                                    currentexam = db.examDao().getExam(exam.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DisplayExam(examId: Int, examName: String?, examSubject: String?, examDate: String?, examDesc: String?, onUpdate: (Exam) -> Unit){
    var edit by remember { mutableStateOf(false) }

    var changeTitle by remember {
        mutableStateOf(examName ?: "")
    }
    var changeSubject by remember {
        mutableStateOf(examSubject ?: "")
    }
    var changeDescription by remember {
        mutableStateOf(examDesc ?: "")
    }
    var selectedDate by remember {
        mutableStateOf(
            examDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
        )
    }

    Card(modifier = Modifier.fillMaxWidth()){
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)){
            IconButton(onClick = {edit = !edit}) { Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")}
            if(edit){
                changeTitle = editTitle(examName)
                changeSubject = editSubject(examSubject)
                DatePickerExample(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                changeDescription = editDescription(examDesc)

                Button(onClick = {
                    edit = false
                    onUpdate(
                        Exam(
                            id = examId,
                            name = changeTitle,
                            subject = changeSubject,
                            date = selectedDate,
                            description = changeDescription
                        )
                    )
                }) { Text("Spara ändringar") }
                Button(onClick = { edit = false }) { Text("Avbryt") }
            }
            else{
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = examName ?: "Ingen titel",
                                style = MaterialTheme.typography.displaySmall,
                                lineHeight = 42.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(50)
                            ) {

                                Text(
                                    text = examSubject ?: "Inget ämne",
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        tonalElevation = 6.dp
                    ) {

                        Column(
                            modifier = Modifier.padding(28.dp)
                        ) {

                            Text(
                                text = "PROVDATUM",
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = examDate ?: "Inget datum",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Anteckningar",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        tonalElevation = 2.dp
                    ) {

                        Text(
                            text = examDesc ?: "Ingen beskrivning",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editTitle(currentTitle: String?): String{
    var newTitle by remember {mutableStateOf(currentTitle?: "")}

    OutlinedTextField(
        value = newTitle,
        onValueChange = { newTitle = it },
        label = { Text("Provnamn") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)
    )

    return newTitle
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editDescription(currentDesc: String?): String{
    var newTitle by remember {mutableStateOf(currentDesc?: "")}

    OutlinedTextField(
        value = newTitle,
        onValueChange = { newTitle = it },
        label = {Text("Beskrivning")},
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        placeholder = { Text("Skriv en beskrivning...") },
        maxLines = 10
    )


    return newTitle
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editSubject(currentSubject: String?): String{
    val subjects = listOf(
        "Matte",
        "Fysik",
        "Programmering"
    )
    var selectedSubject by remember {
        mutableStateOf(currentSubject ?: subjects[0])
    }
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.padding(bottom = 30.dp, end = 100.dp)) {
        OutlinedTextField(
            value = selectedSubject,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Ämne")
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            }
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
    return selectedSubject
}
