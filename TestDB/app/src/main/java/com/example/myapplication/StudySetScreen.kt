package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class StudySetScreen : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            StudySetDatabase::class.java,
            "studyset_database"
        )            .fallbackToDestructiveMigration()
            .build()

        val studySetId = intent.getIntExtra("studyset_id", -1)

        setContent {
            var currentstudyset by remember {
                mutableStateOf<StudySet?>(null)
            }
            LaunchedEffect(Unit) {
                currentstudyset = db.studySetDao().getExam(studySetId)
            }

            MyApplicationTheme {
                val scope = rememberCoroutineScope()
                var playflashcard by remember {mutableStateOf(false)}
                var studysets by remember {
                    mutableStateOf<List<StudySet>>(emptyList())
                }

                val flashCardPairs = remember {
                    mutableStateListOf<Pair<String, String>>()
                }

                // Load exams once
                LaunchedEffect(Unit) {
                    studysets = db.studySetDao().getAll()

                    val result = db.studySetConnDao().getAll(studySetId)

                    flashCardPairs.clear()
                    flashCardPairs.addAll(
                        result.map {
                            it.term to it.definition
                        }
                    )
                }

                Scaffold() {
                    Column(
                        modifier = Modifier.padding(top = 20.dp, start = 20.dp)
                    ) {
                        if(!playflashcard){Button(onClick = {playflashcard = true}) { Text("Starta") }}
                        else{Button(onClick = {playflashcard = false}) { Text("Avbryt") }}

                        if(playflashcard){
                            flascard(currentstudyset, flashCardPairs)
                        }
                        else{
                            editFlashCard(currentstudyset, flashCardPairs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun flascard(studyset: StudySet?, pairs: List<Pair<String, String>>){
    var shuffledPairs = remember { pairs.shuffled().toMutableStateList() }
    var currentTerm by remember {mutableStateOf("")}
    if (shuffledPairs.isNotEmpty()) {

        val currentPair = shuffledPairs.first()

        Column {

            Text(text = currentPair.second)

            OutlinedTextField(
                value = currentTerm,
                onValueChange = { currentTerm = it },
                label = { Text("Term") }
            )

            Button(
                onClick = {
                    if (currentTerm.equals(currentPair.first, ignoreCase = true)) {
                        shuffledPairs.removeAt(0)
                    }

                    else {
                        val wrongCard = shuffledPairs.removeAt(0)
                        shuffledPairs.add(wrongCard)
                    }
                    currentTerm = ""
                }
            ) { Text("Svara") }
        }
    }

    else {

        Text("Alla kort klara 🎉")
    }
}

@Composable
fun editFlashCard(studyset: StudySet?, pairs: List<Pair<String, String>>){

}